package com.example.android.newsapp_2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

//Creating a RecyclerView.Adapter uses code from this video tutorial:
//https://www.youtube.com/watch?v=gGFvbvkZiMs
//Please refer to the tutorial for more information on creating RecyclerView.Adapters

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    //Declare the list of objects that shall be displayed.
    private List<News> newsList;
    //Declare the context object.
    private Context context;
    private OnItemClickListener mListener;


    //Constructor to initialize the newsList and context object.
    public NewsAdapter(List<News> newsList, Context context) {
        this.newsList = newsList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    //Define method to clear the List, includes code from
    // https://stackoverflow.com/questions/29978695/remove-all-items-from-recyclerview
    public void clear() {
        final int size = newsList.size();
        newsList.clear();
        notifyItemRangeRemoved(0, size);
    }

    //Define method to add all Items to the list, includes code from
    //https://stackoverflow.com/questions/31367599/how-to-update-recyclerview-adapter-data
    public void addAll(List<News> newsList) {
        this.newsList.addAll(newsList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //create a view object which equals to the id of your news_item layout with viewgroup parent
        //attachToRoot can be set to either false or true as it is not needed in this case.
        View newsItemViewHolder = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.news_item, parent, false);
        return new ViewHolder(newsItemViewHolder);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //get the current Item (News) from the list.
        News newsItem = newsList.get(position);

        //Get the text for the news items for the different views.
        holder.titleTextView.setText(newsItem.getTitle());
        holder.sectionTextView.setText(newsItem.getSection());
        holder.dateTextView.setText(newsItem.getDate());
        holder.authorTextView.setText(newsItem.getAuthor());

    }

    //Return the size of newsList
    @Override
    public int getItemCount() {
        return newsList.size();
    }

    //Creating an OnItemClickListener for a customized Adapter uses code from this tutorial:
    //https://www.youtube.com/watch?v=bhhs4bwYyhc
    //Please refer to the source for more information
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        //Declare the variables for the different views we want to use in the ViewHolder as
        //public views.
        public TextView titleTextView;
        public TextView sectionTextView;
        public TextView dateTextView;
        public TextView authorTextView;


        public ViewHolder(View itemView) {
            super(itemView);

            //Initialize the views by ID.
            titleTextView = itemView.findViewById(R.id.title);
            sectionTextView = itemView.findViewById(R.id.section);
            dateTextView = itemView.findViewById(R.id.date);
            authorTextView = itemView.findViewById(R.id.author);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

}
