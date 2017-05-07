package com.relay.relay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.Util.GifImageView;
import com.relay.relay.Util.GridSpacingItemDecoration;
import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.LiveQueryAdapter;
import com.relay.relay.Util.SearchContactAdapter;
import com.relay.relay.Util.SearchUser;
import com.relay.relay.Util.ShowActivityFullImage;
import com.relay.relay.Util.StatusBar;
import com.relay.relay.Util.UuidGenerator;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static com.couchbase.lite.replicator.RemoteRequestRetry.TAG;
import static com.relay.relay.DB.InboxDB.REFRESH_INBOX_DB;
import static com.relay.relay.MainActivity.REQUEST_FOR_MANUAL_HAND_SHAKE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InboxFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * create an instance of this fragment.
 */
public class InboxFragment extends Fragment {

    private final String TAG = "RELAY_DEBUG: "+ InboxFragment.class.getSimpleName();

    private Menu mMenu;

    // views in fragments
    private FloatingActionButton fab;

    // fragment view
    private View view = null;

    // waiting gif
    private GifImageView mGifImageView;

    // OnFragmentInteractionListener
    private OnFragmentInteractionListener mListener;

    // database
    DataManager mDataManager;
    Database mDataBase;

    //contacts view and adapter
    private RecyclerView mContactRecyclerView;
    // contact list adapter
    private LiveQuery listsLiveQuery = null;
    private ListAdapter mAdapter;

    // search view and adapter
    private RecyclerView mSearchContactRecyclerView;
    private ArrayList<SearchUser> mSearchContactArrayList;
    private SearchListAdapter mSearchListAdapter;

    // Listener when new update arrived (new msg or update) to refresh contacts database
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;


    public InboxFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG,"On create");
        // To enable editing the tool bar from fragment
        setHasOptionsMenu(true);

        mDataManager = new DataManager(getContext());
        mDataBase = mDataManager.getInboxDB().getDatabase();
        setupLiveQuery();
        mAdapter = new ListAdapter(getContext(), listsLiveQuery);

        createBroadcastReceiver();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(TAG,"onCreateView");
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_inbox, container, false);


        // TODO DELETE THIS- only fore testing
        final Bitmap pic = BitmapFactory.decodeResource(this.getResources(),R.drawable.view);

        // init fab view
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

//                Snackbar.make(view, "Creating new mail message", Snackbar.LENGTH_SHORT)
//                        .setAction("Action", null).show();
                goToComposeActivity();
            }
        });


        // init contacts view
        mContactRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_contacts);
        mContactRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mContactRecyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(1), true));
        mContactRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mContactRecyclerView.setAdapter(mAdapter);
        mContactRecyclerView.setItemViewCacheSize(20);
        initSwipe();

        // init search contacts view
        mSearchContactRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_search_contacts);
        mSearchContactRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSearchContactRecyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(1), true));
        mSearchContactRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mSearchContactRecyclerView.setVisibility(View.GONE);
        mSearchContactRecyclerView.setItemViewCacheSize(20);


//        if (test == 0) {
//            mGifImageView = (GifImageView) view.findViewById(R.id.gif_empty);
//            mGifImageView.setGifImageResource(R.drawable.empty);
//        }

        return view;
    }



    // TODO: Rename method, update argument and hook method into UI event
    public void updateMainactivity(String string) {
        if (mListener != null) {
            mListener.onFragmentInteraction(string);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mDataManager.closeAllDataBase();
        mDataManager.openAllDataBase();
        Log.e(TAG,"close and opened data base");

    }


    @Override
    public void onAttach(Context context) {
        Log.e(TAG,"onAttach");
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        getActivity().unregisterReceiver(mBroadcastReceiver);

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(String string);
    }


    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_manual_handshake).setVisible(true);
        menu.findItem(R.id.action_search).setVisible(true);
        menu.findItem(R.id.action_approve).setVisible(false);

        // save current menu;
        mMenu = menu;

        final MenuItem search = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(search);

        MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {

                mSearchContactArrayList = mDataManager.getInboxDB().getUserList(mDataManager);
                mSearchListAdapter = new SearchListAdapter(mSearchContactArrayList);
                mSearchContactRecyclerView.setAdapter(mSearchListAdapter);

                mSearchContactRecyclerView.setVisibility(View.VISIBLE);
                mContactRecyclerView.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchContactRecyclerView.setVisibility(View.GONE);
                mContactRecyclerView.setVisibility(View.VISIBLE);
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchContactRecyclerView.setVisibility(View.GONE);
                menu.findItem(R.id.action_search).collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mSearchListAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_manual_handshake) {
            Snackbar.make(view, "Manual handshake request", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            startManualSync();
            return true;
        }

        if (id == R.id.home) {

        }

        return super.onOptionsItemSelected(item);
    }

    public void startManualSync(){
        //  BroadCast to service
        Intent updateActivity = new Intent(RelayConnectivityManager.MANUAL_SYNC);
        getActivity().sendBroadcast(updateActivity);
    }


    private void setupLiveQuery() {
        if (mDataBase == null) {
            Log.e(TAG,"Error, mDataBase is null!");
            return;
        }
        com.couchbase.lite.View listsView = mDataBase.getView("list/contactList");
        if (listsView.getMap() == null) {
            listsView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    String type = (String) document.get("type");
                    if ("contact".equals(type)) {
                        if (!(boolean) document.get("disappear"))
                        emitter.emit(document.get("time"), null);
                    }
                }
            }, "1.0");
        }
        Log.e(TAG,"how many rows : "+listsView.getCurrentTotalRows());
        Query query = listsView.createQuery();
        query.setDescending(true);
        listsLiveQuery = query.toLiveQuery();
    }


    private class ListAdapter extends LiveQueryAdapter {


        public ListAdapter(Context context, LiveQuery query) {
            super(context, query);
        }

        @Override
        public InboxFragment.ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_contact, parent, false);

            return new ContactViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final InboxFragment.ContactViewHolder holder, int position) {

            Map<String,Object> properties = enumerator.getRow(position).getDocument().getProperties();

            final String uuidString = (String) properties.get("uuid");
            boolean newMessages = (boolean) properties.get("new_messages");
            boolean updates = (boolean) properties.get("updates");
            String lastMessage = (String) properties.get("last_message");
            //boolean disappear = (boolean) properties.get("disappear");
            String time = (String) properties.get("time");
            String email ="";

            // get node from nodeDB
            final Node node = mDataManager.getNodesDB().getNode(UUID.fromString(uuidString));
            // if user not exist in my mesh(nodeDB) put the mail and unknown user picture profile
            if (node == null){
                holder.circleImageView.setImageResource(R.drawable.pic_unknown_user);
                UuidGenerator uuidGenerator = new UuidGenerator();
                email =uuidGenerator.GenerateEmailFromUUID(UUID.fromString(uuidString));
                holder.userName.setText(email);
            }
            else{
                holder.userName.setText(node.getFullName()+", @"+node.getUserName());
                // scale down image quality
                Bitmap newImage = ImageConverter.convertBytesToBitmap(node.getProfilePicture());
                if( newImage == null )
                    holder.circleImageView.setImageResource(R.drawable.pic_unknown_user);
                else{
                    newImage = ImageConverter.scaleDown(newImage,100,true);
                    holder.circleImageView.setImageBitmap(newImage);
                }
            }

            // set updates and new messages icon
            if (newMessages)
                holder.newMsgs.setVisibility(View.VISIBLE);
            else
                holder.newMsgs.setVisibility(View.INVISIBLE);
            // todo disable update - do not delete
//            if (updates)
//                holder.updates.setVisibility(View.VISIBLE);
//            else
//                holder.updates.setVisibility(View.INVISIBLE);

            // set time
            time = convertTimeToReadableString(time);
            holder.time.setText(time);

            // set last msg
            if(lastMessage != null)
                holder.lastMsg.setText(lastMessage);

            // listener to contact setting
            holder.settingContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(holder.settingContact,holder.userName.getText().toString(),uuidString);
                }
            });

            // listener to contact click area
            holder.lastMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDataManager.getInboxDB().setContactSeenByUser(UUID.fromString(uuidString));
                    if (node != null)
                        goToConversationActivity(UUID.fromString(uuidString),node.getFullName());
                    else
                        goToConversationActivity(UUID.fromString(uuidString),
                                new UuidGenerator().GenerateEmailFromUUID(UUID.fromString(uuidString)));
                }
            });
            holder.userName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDataManager.getInboxDB().setContactSeenByUser(UUID.fromString(uuidString));
                    if (node != null)
                        goToConversationActivity(UUID.fromString(uuidString),node.getFullName());
                    else
                        goToConversationActivity(UUID.fromString(uuidString),
                                new UuidGenerator().GenerateEmailFromUUID(UUID.fromString(uuidString)));

                }
            });

            // listener to profile picture
            holder.circleImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ( node == null)
                        new ShowActivityFullImage(null,getActivity());
                    else
                        new ShowActivityFullImage(ImageConverter.convertBytesToBitmap(node.getProfilePicture()),getActivity());
                }
            });
        }
    }

    private void goToConversationActivity(UUID uuid, String name) {
        Intent intent = new Intent(getContext(), ConversationActivity.class);
        intent.putExtra(MainActivity.USER_NAME, name);
        intent.putExtra(MainActivity.USER_UUID, uuid.toString());
        startActivity(intent);
    }
    private void goToComposeActivity() {
        Intent intent = new Intent(getContext(), ComposeMessageActivity.class);
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.push_up_start, R.anim.push_up_end);
    }



    private String convertTimeToReadableString(String time){
        String year = time.substring(0,4);
        String month = time.substring(4,6);
        String day = time.substring(6,8);
        String hour = time.substring(8,10);
        String min = time.substring(10,12);
        return day+"/"+month+"  "+hour+":"+min;
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {
        // Views
        public TextView userName;
        public TextView lastMsg;
        public TextView time;
        public ImageView settingContact;
        // todo disable update - do not delete
       // public ImageView updates;
        public ImageView newMsgs;
        public de.hdodenhof.circleimageview.CircleImageView circleImageView;


        public ContactViewHolder(View view) {
            super(view);

            userName = (TextView) view.findViewById(R.id.textView_item_contact_name);
            lastMsg = (TextView) view.findViewById(R.id.textView_item_contact_last_message);
            time = (TextView) view.findViewById(R.id.textView_item_contact_last_time);
            settingContact = (ImageView) view.findViewById(R.id.imageView_setting_contact);
            // todo disable update - do not delete
           // updates = (ImageView) view.findViewById(R.id.imageView_item_contact_updates);
            newMsgs = (ImageView) view.findViewById(R.id.imageView_item_contact_new_messages);
            circleImageView = (de.hdodenhof.circleimageview.CircleImageView)
                    view.findViewById(R.id.dialog_image);
        }

    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    /**
     * Showing popup menu when tapping on 3 dots
     */
    private void showPopupMenu(View view,String name,String uuidString) {
        // inflate menu
        PopupMenu popup = new PopupMenu(getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.contact_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new MyMenuItemClickListener(name,uuidString));
        popup.show();
    }

    /**
     * Click listener for popup menu items
     */
    class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        String name;
        String uuidString;

        public MyMenuItemClickListener(String name,String uuidString) {
            this.name = name;
            this.uuidString = uuidString;
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_user_info:
                    Toast.makeText(getContext(), "User "+name+" info ", Toast.LENGTH_SHORT).show();
                    updateMainactivity(uuidString);

                    return true;
                case R.id.action_delete_conversation:
                    mDataManager.getInboxDB().deleteUserAndConversation(UUID.fromString(uuidString),false);
                    Toast.makeText(getContext(), "Delete "+name+" conversation", Toast.LENGTH_SHORT).show();
                    return true;
                default:
            }
            return false;
        }
    }


    private void initSwipe(){

         final Paint p = new Paint();

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (direction == ItemTouchHelper.LEFT){
                    Document doc =  mAdapter.getItem(position);
                    if(doc != null) {
                        Map<String, Object> properties = doc.getProperties();
                        String uuidString = (String) properties.get("uuid");
                        mDataManager.getInboxDB().deleteUserAndConversation(UUID.fromString(uuidString),true);
                        Toast.makeText(getContext(), "user was deleted", Toast.LENGTH_SHORT).show();
                        mAdapter.myNotify();

                    }
                } else {
                    Document doc =  mAdapter.getItem(position);
                    if(doc != null) {
                        Map<String, Object> properties = doc.getProperties();
                        String uuidString = (String) properties.get("uuid");
                        updateMainactivity(uuidString);
                        Toast.makeText(getContext(), "user info", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                Bitmap icon;
                if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;
                    float widthText = height/2;

                    if(dX > 0){
                        p.setColor(Color.parseColor("#1388ab"));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX,(float) itemView.getBottom());
                        c.drawRect(background,p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_user_information);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + width ,(float) itemView.getTop() + width,(float) itemView.getLeft()+ 2*width,(float)itemView.getBottom() - width);
                        c.drawBitmap(icon,null,icon_dest,p);
                        p.setColor(Color.parseColor("#FFFFFF"));
                        p.setTextSize(30);
                        c.drawText("User info",(float) itemView.getLeft() + widthText/3,(float)itemView.getBottom()-widthText/4,p);
                    } else {
                        p.setColor(Color.parseColor("#D32F2F"));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(),(float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background,p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_trash_can);
                        RectF icon_dest = new RectF((float) itemView.getRight() - 2*width ,(float) itemView.getTop() + width,(float) itemView.getRight() - width,(float)itemView.getBottom() - width);
                        c.drawBitmap(icon,null,icon_dest,p);
                        p.setColor(Color.parseColor("#FFFFFF"));
                        p.setTextSize(30);
                        c.drawText("Delete",(float) itemView.getRight() - widthText*6/4,(float)itemView.getBottom()-widthText/4,p);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mContactRecyclerView);
    }

    private void removeView(){
        if(view.getParent()!=null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }


    public class SearchListAdapter extends SearchContactAdapter {

        public SearchListAdapter(ArrayList<SearchUser> arrayList) {
            super(arrayList);
        }

        @Override
        public SearchContactAdapter.SearchViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_contact_search_result, viewGroup, false);
            return new SearchViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SearchContactAdapter.SearchViewHolder viewHolder,final int i) {

            if (mFilteredList.get(i).getUserName() == ""){
                viewHolder.contact.setText(mFilteredList.get(i).getEmail());
            }
            else{
                viewHolder.contact.setText(mFilteredList.get(i).getFullName()+",  @"+mFilteredList.get(i).getUserName());
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    goToConversationActivity(mFilteredList.get(i).getUuid(),mFilteredList.get(i).getFullName());
                    mSearchContactRecyclerView.setVisibility(View.GONE);
                    mMenu.findItem(R.id.action_search).collapseActionView();
                }
            });
        }

    }


    /**
     * BroadcastReceiver
     */
    private  void createBroadcastReceiver() {
        mFilter = new IntentFilter();
        mFilter.addAction(REFRESH_INBOX_DB);
        mFilter.addAction(REQUEST_FOR_MANUAL_HAND_SHAKE);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                switch (action){

                    case REFRESH_INBOX_DB:
                        mDataManager.closeAllDataBase();
                        mDataManager.openAllDataBase();
                        Log.e(TAG,"close and opened data base");
                        setupLiveQuery();
                        mAdapter.myNotify();


                        break;

                    case REQUEST_FOR_MANUAL_HAND_SHAKE:
                        String msg = intent.getStringExtra("message");
                        createAlertDialog("Manual hand shake",msg);
                        break;
                }
            }
        };
        getActivity().registerReceiver(mBroadcastReceiver, mFilter);
    }


    /**
     *  Create alert dialog
     */
    private void createAlertDialog(String title,String msg) {

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();
    }
}
