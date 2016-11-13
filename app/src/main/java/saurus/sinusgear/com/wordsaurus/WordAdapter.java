package saurus.sinusgear.com.wordsaurus;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * Created by georgik on 8/28/16.
 */

public class WordAdapter extends SimpleAdapter {

    private final LayoutInflater mInflater;
    private List<? extends Map<String, ?>> mData;
    private int mResource;

    /**
     * Constructor
     *
     * @param context  The context where the View associated with this SimpleAdapter is running
     * @param data     A List of Maps. Each entry in the List corresponds to one row in the list. The
     *                 Maps contain the data for each row, and should include all the entries specified in
     *                 "from"
     * @param resource Resource identifier of a view layout that defines the views for this list
     *                 item. The layout file should include at least those named views defined in "to"
     * @param from     A list of column names that will be added to the Map associated with each
     *                 item.
     * @param to       The views that should display column in the "from" parameter. These should all be
     *                 TextViews. The first N views in this list are given the values of the first N columns
     */
    public WordAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);
        mData = data;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }

        LinearLayout wordView = (LinearLayout) view.findViewById(R.id.wordView);
        TextView wordKeyView = (TextView) view.findViewById(R.id.wordKey);
        TextView wordDescriptionView = (TextView) view.findViewById(R.id.wordDescription);


        final Map dataSet = mData.get(position);

        final Object data = dataSet.get("key");
        wordKeyView.setText(data.toString());

        final Object descr = dataSet.get("descr");
        wordDescriptionView.setText(Html.fromHtml(descr.toString()));

        final Object rank = dataSet.get("rank");
        if (rank != null) {
            wordView.setBackgroundColor(Color.parseColor(rank.toString()));
        } else {
            wordView.setBackgroundColor(Color.TRANSPARENT);
        }

        return view;
    }
}
