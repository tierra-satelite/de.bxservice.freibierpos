package de.bxservice.bxpos.logic;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.bxservice.bxpos.logic.model.idempiere.DefaultPosData;
import de.bxservice.bxpos.logic.model.idempiere.MProduct;
import de.bxservice.bxpos.logic.model.idempiere.RestaurantInfo;
import de.bxservice.bxpos.logic.model.idempiere.ProductCategory;
import de.bxservice.bxpos.logic.model.idempiere.ProductPrice;
import de.bxservice.bxpos.logic.model.idempiere.Table;
import de.bxservice.bxpos.logic.model.idempiere.TableGroup;
import de.bxservice.bxpos.logic.print.POSOutputDevice;
import de.bxservice.bxpos.logic.webservices.DefaultPosDataWebServiceAdapter;
import de.bxservice.bxpos.logic.webservices.OrgInfoWebServiceAdapter;
import de.bxservice.bxpos.logic.webservices.OutputDeviceWebServiceAdapter;
import de.bxservice.bxpos.logic.webservices.ProductCategoryWebServiceAdapter;
import de.bxservice.bxpos.logic.webservices.ProductPriceWebServiceAdapter;
import de.bxservice.bxpos.logic.webservices.ProductWebServiceAdapter;
import de.bxservice.bxpos.logic.webservices.TableWebServiceAdapter;

/**
 * Class that reads from iDempiere the data and call the necessary methods
 * to persist it in the database
 * Created by Diego Ruiz on 6/11/15.
 */
public class DataReader {

    private static final String LOG_TAG = "Data Reader";

    private List<ProductCategory> productCategoryList = new ArrayList<>();
    private List<TableGroup> tableGroupList = new ArrayList<>();
    private List<MProduct> productList = new ArrayList<>();
    private List<ProductPrice> productPriceList = new ArrayList<>();
    private DefaultPosData defaultData = null;
    private RestaurantInfo restaurantInfo = null;
    private List<POSOutputDevice> outputDeviceList = new ArrayList<>();
    private boolean error = false;
    private Context mContext;


    public DataReader(Context ctx) {

        mContext = ctx;

        Thread outputDeviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputDeviceWebServiceAdapter deviceWS = new OutputDeviceWebServiceAdapter();
                outputDeviceList = deviceWS.getOutputDeviceList();
                persistDeviceList();
            }
        });

        outputDeviceThread.run();

        Thread productCategoryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ProductCategoryWebServiceAdapter productCategoryWS = new ProductCategoryWebServiceAdapter();
                productCategoryList = productCategoryWS.getProductCategoryList();

                ProductWebServiceAdapter productWS = new ProductWebServiceAdapter();
                productList = productWS.getProductList();

                ProductPriceWebServiceAdapter productPriceWS = new ProductPriceWebServiceAdapter();
                productPriceList = productPriceWS.getProductPriceList();

                setProductRelations();
                persistProductAttributes();
            }
        });

        productCategoryThread.run();

        Thread tableThread = new Thread(new Runnable() {
            @Override
            public void run() {
                TableWebServiceAdapter tableWS = new TableWebServiceAdapter();
                tableGroupList = tableWS.getTableGroupList();
                persistTables();
            }
        });

        tableThread.run();

        Thread tablePosData = new Thread(new Runnable() {
            @Override
            public void run() {
                DefaultPosDataWebServiceAdapter dataWS = new DefaultPosDataWebServiceAdapter();
                defaultData = dataWS.getDefaultPosData();
                persistPosData();
            }
        });

        tablePosData.run();

        Thread orgInfoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OrgInfoWebServiceAdapter dataWS = new OrgInfoWebServiceAdapter();
                restaurantInfo = dataWS.getRestaurantInfo();
                persistOrgInfo();
            }
        });

        orgInfoThread.run();

    }

    /**
     * Save default data in the database
     */
    private void persistPosData() {
        defaultData.saveData(mContext);
    }

    /**
     * Save default data in the database
     */
    private void persistOrgInfo() {
        restaurantInfo.saveData(mContext);
    }

    private void persistDeviceList() {
        for(POSOutputDevice device : outputDeviceList) {
            device.save(mContext);
        }

    }

    /**
     * Save tables in the database
     */
    private void persistTables() {
        for(TableGroup tg : tableGroupList) {
            tg.save(mContext);
            for(Table table : tg.getTables()) {
                table.save(mContext);
            }
        }
    }

    /**
     * Save product attributes in the database
     */
    private void persistProductAttributes() {
        for(ProductCategory productCategory : productCategoryList)
            productCategory.save(mContext);

        for(MProduct product : productList)
            product.save(mContext);

        for(ProductPrice productPrice : productPriceList)
            productPrice.save(mContext);
    }

    public boolean isError() {
        return error;
    }

    public boolean isDataComplete(){

        if(productCategoryList  != null && !productCategoryList.isEmpty() &&
                productList      != null && !productList.isEmpty() &&
                tableGroupList   != null && !tableGroupList.isEmpty() &&
                productPriceList != null && !productPriceList.isEmpty() &&
               defaultData != null) {
            return true;
        }

        Log.e(LOG_TAG, "missing data");

        return false;
    }

    /**
     * Set the relation between product category and its respective products
     */
    private void setProductRelations(){

        //Relation between product category and product
        if(productCategoryList != null && !productCategoryList.isEmpty() &&
                productList != null && !productList.isEmpty()) {

            int productCategoryId;
            int childProductCategoryId;
            for(ProductCategory productCategory : productCategoryList) {

                productCategoryId = productCategory.getProductCategoryID();
                for(MProduct product : productList) {
                    childProductCategoryId = product.getProductCategoryId();
                    if(childProductCategoryId == productCategoryId)
                        productCategory.getProducts().add(product);
                }
            }
        }
        else {
            Log.e(LOG_TAG, "missing products");
            error = true;
        }

        //Relation between products and prices - the list has to be the same long. One price per every product
        if( productPriceList != null && !productPriceList.isEmpty() &&
                productList != null && !productList.isEmpty() &&
                productPriceList.size() == productList.size() ){

            int productId;
            int priceProductId;
            for(ProductPrice productPrice : productPriceList) {

                priceProductId = productPrice.getProductID();
                for(MProduct product : productList) {
                    productId = product.getProductID();
                    if(priceProductId == productId) {
                        productPrice.setProduct(product);
                    }
                }
            }
        }
        else {
            Log.e(LOG_TAG, "missing price products");
            error = true;
        }

    }
}
