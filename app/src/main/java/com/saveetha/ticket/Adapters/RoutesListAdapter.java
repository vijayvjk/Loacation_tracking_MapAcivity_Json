package com.saveetha.ticket.Adapters;



import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.saveetha.ticket.Models.Route;
import com.saveetha.ticket.R;

import java.util.ArrayList;
import java.util.List;



public class RoutesListAdapter extends RecyclerView
        .Adapter<RoutesListAdapter
        .DataObjectHolder> {

    private static String LOG_TAG = "RoutesListAdapter";
    private ArrayList<Route> mDataset;
    private static MyClickListener myClickListener;

    private Context mContext;

    String[] colors = { "#F38181", "#08D9D6", "#AA96DA", "#88304E", "#FF9999"};

    public static class DataObjectHolder extends RecyclerView.ViewHolder
            implements View
            .OnClickListener {

        TextView route_number;
        TextView route_info;
        TextView stages;

        public DataObjectHolder(View itemView) {
            super(itemView);

            route_number = (TextView) itemView.findViewById(R.id.route_name);
            route_info = (TextView) itemView.findViewById(R.id.route_info);
            stages = (TextView) itemView.findViewById(R.id.stages);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            myClickListener.onItemClick(getAdapterPosition(), v);
        }
    }

    public void setOnItemClickListener(MyClickListener myClickListener) {
        this.myClickListener = myClickListener;
    }

    public RoutesListAdapter(ArrayList<Route> myDataset, Context context) {
        mContext = context;
        mDataset = myDataset;
    }


    @Override
    public DataObjectHolder onCreateViewHolder(ViewGroup parent,
                                               int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.routes_row, parent, false);

        DataObjectHolder dataObjectHolder = new DataObjectHolder(view);
        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(DataObjectHolder holder, int position) {

        holder.route_number.setText(mDataset.get(position).getName());
        holder.route_number.setBackgroundColor(Color.parseColor(colors[position]));
        holder.route_info.setText(mDataset.get(position).getOrigin().toUpperCase() + " - "+ mDataset.get(position).getDestination().toUpperCase());

        //String stages = "";
        List<String> stages = new ArrayList<>();

        for(int i=0;i<mDataset.get(position).getStages().size();i++) {
            stages.add(mDataset.get(position).getStages().get(i).getName().toUpperCase());
            //stages += mDataset.get(position).getStages().get(i).getName() + " , ";
        }

        holder.stages.setText("Via : " + join(stages));

    }

    public static String join(List<String> msgs) {
        return msgs == null || msgs.size() == 0 ? "" : msgs.size() == 1 ? msgs.get(0) : msgs.subList(0, msgs.size() - 1).toString().replaceAll("^.|.$", "") + " and " + msgs.get(msgs.size() - 1);
    }


    public void addItem(Route dataObj, int index) {
        mDataset.add(index, dataObj);
        notifyItemInserted(index);
    }

    public void deleteItem(int index) {
        mDataset.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public interface MyClickListener {
        public void onItemClick(int position, View v);
    }
}