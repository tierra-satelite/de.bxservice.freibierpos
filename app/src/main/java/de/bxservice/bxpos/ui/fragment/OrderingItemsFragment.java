/**********************************************************************
 * This file is part of FreiBier POS                                   *
 *                                                                     *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Diego Ruiz - Bx Service GmbH                                      *
 **********************************************************************/
package de.bxservice.bxpos.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.bxservice.bxpos.R;
import de.bxservice.bxpos.logic.model.pos.POSOrder;
import de.bxservice.bxpos.logic.model.pos.POSOrderLine;
import de.bxservice.bxpos.ui.EditOrderActivity;
import de.bxservice.bxpos.ui.RecyclerItemsListener;
import de.bxservice.bxpos.ui.adapter.EditPagerAdapter;
import de.bxservice.bxpos.ui.adapter.OnDataChangeListener;
import de.bxservice.bxpos.ui.adapter.OrderingLineAdapter;
import de.bxservice.bxpos.ui.adapter.SimpleItemTouchHelperCallback;
import de.bxservice.bxpos.ui.decorator.DividerItemDecoration;

/**
 * Created by Diego Ruiz on 10/12/15.
 */
public class OrderingItemsFragment extends Fragment {

    private static final String ARG_ORDER = "related_order";

    private POSOrder order;

    private RecyclerView mRecyclerView;
    private OrderingLineAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static OrderingItemsFragment newInstance(POSOrder order) {
        OrderingItemsFragment fragment = new OrderingItemsFragment();
        Bundle args = new Bundle();

        args.putSerializable(ARG_ORDER, order);

        fragment.setArguments(args);
        return fragment;
    }

    public OrderingItemsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_edit_order, container, false);

        order = (POSOrder) getArguments().getSerializable(ARG_ORDER);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (and its listener)
        mAdapter = new OrderingLineAdapter(new ArrayList<>(order.getOrderingLines()));

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity().getBaseContext(), DividerItemDecoration.VERTICAL_LIST));

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemsListener(getActivity().getBaseContext(), mRecyclerView, new RecyclerItemsListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        int idx = mRecyclerView.getChildAdapterPosition(view);
                        ((EditOrderActivity) getActivity()).onClickPressed(idx, EditPagerAdapter.ORDERING_POSITION);
                    }

                    @Override
                    public void onItemLongClick(View view, int position)
                    {
                        //POSOrderLine selectedItem = mAdapter.getSelectedItem(position);
                        int idx = mRecyclerView.getChildAdapterPosition(view);
                        ((EditOrderActivity) getActivity()).onLongPressed(idx, EditPagerAdapter.ORDERING_POSITION);
                    }
                })
        );

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);

        mAdapter.setOnDataChangeListener(new OnDataChangeListener() {
            @Override
            public void onItemDeleted(int position) {
                ((EditOrderActivity) getActivity()).removeItem(position);
            }

            @Override
            public void onItemAdded(int position, Object object) {
                ((EditOrderActivity) getActivity()).addItem(position, (POSOrderLine) object);
            }
        });

        return rootView;
    }

    public OrderingLineAdapter getmAdapter() {
        return mAdapter;
    }

    public void refresh(POSOrder order) {
        this.getArguments().putSerializable(ARG_ORDER, order);
        mAdapter.notifyDataSetChanged();
    }

}