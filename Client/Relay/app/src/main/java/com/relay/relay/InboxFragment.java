package com.relay.relay;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
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

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.Util.GridSpacingItemDecoration;
import com.relay.relay.Util.LiveQueryAdapter;
import com.relay.relay.Util.UuidGenerator;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.util.Map;
import java.util.UUID;

import static com.couchbase.lite.replicator.RemoteRequestRetry.TAG;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InboxFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InboxFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InboxFragment extends Fragment {
//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    // views in fragments
    private FloatingActionButton fab;

    // feagment view
    private View view = null;

    // OnFragmentInteractionListener
    private OnFragmentInteractionListener mListener;

    // database
    DataManager mDataManager;
    Database mDataBase;


    private RecyclerView contactRecyclerView;
    // contact list adapter
    private LiveQuery listsLiveQuery = null;
    private ListAdapter mAdapter;


    public InboxFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InboxFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InboxFragment newInstance(String param1, String param2) {
        InboxFragment fragment = new InboxFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG,"On create");
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
        // To enable editing the tool bar from fragment
        setHasOptionsMenu(true);
        mDataManager = new DataManager(getContext());
        mDataBase = mDataManager.getInboxDB().getDatabase();
        setupLiveQuery();
        mAdapter = new ListAdapter(getContext(), listsLiveQuery);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(TAG,"onCreateView");
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_inbox, container, false);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        contactRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_contacts);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Creating new mail message", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                UuidGenerator uuidG= new UuidGenerator();
                RelayMessage m = null;
                try {
                    m = new RelayMessage(uuidG.GenerateUUIDFromEmail("rachael@gmail.com"),mDataManager.getNodesDB().getMyNodeId(),
                            RelayMessage.TYPE_MESSAGE_TEXT,"this msg with txt");
                    mDataManager.getMessagesDB().addMessage(m);
                //    mDataManager.getInboxDB().updateContactItem(uuidG.GenerateUUIDFromEmail("Rachael@gmail.com"),true,true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        contactRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        contactRecyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(1), true));
        contactRecyclerView.setItemAnimator(new DefaultItemAnimator());
        contactRecyclerView.setAdapter(mAdapter);
        initSwipe();

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String string) {
        if (mListener != null) {
            mListener.onFragmentInteraction(string);
        }
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
//        if (listsLiveQuery != null) {
//            listsLiveQuery.stop();
//            listsLiveQuery = null;
//        }
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
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_manual_handshake).setVisible(true);
        menu.findItem(R.id.action_search).setVisible(true);
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
        if (id == R.id.action_search) {
            Snackbar.make(view, "Search", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            return true;
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
                    boolean disappear = (boolean) document.get("disappear");
                    if ("contact".equals(type) && !disappear) {
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
            //boolean disappear = (boolean) properties.get("disappear");
            String time = (String) properties.get("time");

            // get node from nodeDB
            Node node = mDataManager.getNodesDB().getNode(UUID.fromString(uuidString));
            // if user not exist in my mesh(nodeDB) put the mail and unknown user picture profile
            if (node == null){
                holder.circleImageView.setImageResource(R.drawable.pic_unknown_user);
                UuidGenerator uuidGenerator = new UuidGenerator();
                String email =uuidGenerator.GenerateEmailFromUUID(UUID.fromString(uuidString));
                holder.userName.setText(email);
            }
            else{
                holder.userName.setText("@"+node.getUserName()+" ,"+node.getFullName());
                holder.circleImageView.setImageBitmap(node.getProfilePicture());
            }


            // set updates and new messages icon
            if (newMessages)
                holder.newMsgs.setVisibility(View.VISIBLE);
            else
                holder.newMsgs.setVisibility(View.INVISIBLE);
            if (updates)
                holder.updates.setVisibility(View.VISIBLE);
            else
                holder.updates.setVisibility(View.INVISIBLE);

            // set time
            time = convertTimeToReadableString(time);
            holder.time.setText(time);

            // set last msg
            holder.lastMsg.setText("need to finish");

            // listener to contact setting
            holder.settingContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(holder.settingContact,holder.userName.getText().toString());
                }
            });

            // listener to contact click area
            holder.lastMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    goToContactConversationActivity(UUID.fromString(uuidString));
                }
            });

        }
    }

    private void goToContactConversationActivity(UUID uuid) {
        //TODO go to activity
        Toast.makeText(getContext(), "goToContactConversationActivity: "+
                mDataManager.getInboxDB().setContactSeenByUser(uuid), Toast.LENGTH_SHORT).show();

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
        public ImageView updates;
        public ImageView newMsgs;
        public de.hdodenhof.circleimageview.CircleImageView circleImageView;


        public ContactViewHolder(View view) {
            super(view);

            userName = (TextView) view.findViewById(R.id.textView_item_contact_name);
            lastMsg = (TextView) view.findViewById(R.id.textView_item_contact_last_message);
            time = (TextView) view.findViewById(R.id.textView_item_contact_last_time);
            settingContact = (ImageView) view.findViewById(R.id.imageView_setting_contact);
            updates = (ImageView) view.findViewById(R.id.imageView_item_contact_updates);
            newMsgs = (ImageView) view.findViewById(R.id.imageView_item_contact_new_messages);
            circleImageView = (de.hdodenhof.circleimageview.CircleImageView)
                    view.findViewById(R.id.profile_image);
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
    private void showPopupMenu(View view,String name) {
        // inflate menu
        PopupMenu popup = new PopupMenu(getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.contact_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new MyMenuItemClickListener(name));
        popup.show();
    }

    /**
     * Click listener for popup menu items
     */
    class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        String name;
        public MyMenuItemClickListener(String name) {
            this.name = name;
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_user_info:
                    Toast.makeText(getContext(), "User "+name+" info ", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.action_delete_conversation:
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
                    //mAdapter.removeItem(position);
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "delete", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "info", Toast.LENGTH_SHORT).show();
                    mAdapter.notifyDataSetChanged();
//                    removeView();
//                    edit_position = position;
//                    alertDialog.setTitle("Edit Country");
//                    et_country.setText(countries.get(position));
//                    alertDialog.show();
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
        itemTouchHelper.attachToRecyclerView(contactRecyclerView);
    }

    private void removeView(){
        if(view.getParent()!=null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }
}
