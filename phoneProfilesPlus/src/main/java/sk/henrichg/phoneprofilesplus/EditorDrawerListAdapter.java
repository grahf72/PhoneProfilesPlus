package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class EditorDrawerListAdapter extends BaseAdapter {

    private final Context context;
    private final String[] drawerItemsTitle;
    private final String[] drawerItemsSubtitle;
    private final Integer[] drawerItemsIcon;
    
    EditorDrawerListAdapter(/*ListView listView, */Context context,
                                    String[] itemTitle,
                                    String[] itemSubtitle,
                                    Integer[] itemIcon)
    {
        this.context = context;
        this.drawerItemsTitle = itemTitle;
        this.drawerItemsSubtitle = itemSubtitle;
        this.drawerItemsIcon = itemIcon;
    }
    
    public int getCount() {
        return drawerItemsTitle.length;
    }

    public Object getItem(int position) {
        return drawerItemsTitle[position];
    }

    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
          TextView itemTitle;
          TextView itemSubtitle;
          ImageView itemIcon;
          //int position;
        }


    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        View vi = convertView;
        if (convertView == null)
        {
            LayoutInflater inflater = LayoutInflater.from(context);
            //LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            vi = inflater.inflate(R.layout.editor_drawer_list_item, parent, false); 

            holder = new ViewHolder();
            holder.itemTitle = vi.findViewById(R.id.editor_drawer_list_item_title);
            holder.itemSubtitle = vi.findViewById(R.id.editor_drawer_list_item_subtitle);
            holder.itemIcon = vi.findViewById(R.id.editor_drawer_list_item_icon);
            vi.setTag(holder);        
        }
        else
        {
            holder = (ViewHolder)vi.getTag();
        }
        
        holder.itemTitle.setText(drawerItemsTitle[position]);
        holder.itemSubtitle.setText(drawerItemsSubtitle[position]);
        holder.itemIcon.setImageResource(drawerItemsIcon[position]);

        return vi;	
    }

}
