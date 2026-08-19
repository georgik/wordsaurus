package saurus.sinusgear.com.wordsaurus;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

public class WordAdapter extends SimpleAdapter {

    private final LayoutInflater mInflater;
    private final List<? extends Map<String, ?>> mData;
    private final int mResource;

    public WordAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);
        mData = data;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }

        LinearLayout wordView = view.findViewById(R.id.wordView);
        TextView wordKeyView = view.findViewById(R.id.wordKey);
        TextView wordDescriptionView = view.findViewById(R.id.wordDescription);

        final Map dataSet = mData.get(position);

        final Object data = dataSet.get("key");
        wordKeyView.setText(data.toString());

        final Object descr = dataSet.get("descr");
        wordDescriptionView.setText(Html.fromHtml(descr.toString(), Html.FROM_HTML_MODE_LEGACY));

        final Object rank = dataSet.get("rank");
        if (rank != null) {
            wordView.setBackgroundColor(Color.parseColor(rank.toString()));
        } else {
            wordView.setBackgroundColor(Color.TRANSPARENT);
        }

        return view;
    }
}
