package com.relay.relay;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.relay.relay.Util.CountryCodeActivityDialog;
import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.Imageutils;
import com.relay.relay.Util.UuidGenerator;

import java.io.IOException;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;
import static com.relay.relay.SignupActivity.STEP_1_FULL_NAME;
import static com.relay.relay.SignupActivity.STEP_2_USER_NAME;
import static com.relay.relay.SignupActivity.STEP_3_EMAIL;
import static com.relay.relay.SignupActivity.STEP_4_PASSWORD;
import static com.relay.relay.SignupActivity.STEP_5_PICTURE;
import static com.relay.relay.SignupActivity.STEP_6_RESIDENCE;
import static com.relay.relay.SignupActivity.STEP_7_FINISH;
import static com.relay.relay.SignupActivity.STEP_BACK;
import static com.relay.relay.SignupActivity.STEP_NEXT;
import static com.relay.relay.Util.CountryCodeActivityDialog.ACTION_OPEN;
import static com.relay.relay.Util.Imageutils.CAMERA_REQUEST;
import static com.relay.relay.Util.Imageutils.GALLERY_REQUEST;


public class SignupStepFragment extends Fragment {

    private static final String RECEIVED_STEP = "received_step";
    private static final String RECEIVED_OBJECT = "received_object";

    private int step;
    private String inputText;
    private Bitmap inputImage;
    private int countryCode;

    //For Image Attachment
    private Imageutils imageUtils;
    private Bitmap loadedBitmap;
    private Uri loadedUri;

    // view in every fragment
    private TextView textViewStepLabel;
    private ImageView imageViewStepRight;
    private ImageView imageViewStepLeft;

    // input views that change in each step
    private TextView textViewConfirmLabel;
    private ImageView imageViewInput;
    private TextView textViewInput;
    private EditText editTextInput;
    private EditText editTextInput2;
    private de.hdodenhof.circleimageview.CircleImageView circleImageView;


    private OnFragmentInteractionListener mListener;

    public SignupStepFragment() {
        // Required empty public constructor
    }

    public static SignupStepFragment newStepInstance(int step ,Object input) {
        SignupStepFragment fragment = new SignupStepFragment();
        Bundle args = new Bundle();
        args.putInt(RECEIVED_STEP, step);
        if (step == STEP_5_PICTURE)
            args.putByteArray(RECEIVED_OBJECT,(byte[]) ImageConverter.ConvertBitmapToBytes(((Bitmap) input)));
        else if (step == STEP_6_RESIDENCE)
             args.putInt(RECEIVED_OBJECT,(int) input);
             else
                args.putString(RECEIVED_OBJECT,(String)input);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            step = getArguments().getInt(RECEIVED_STEP);
            if (step == STEP_5_PICTURE)
                inputImage = (Bitmap) ImageConverter.convertBytesToBitmap(getArguments().getByteArray(RECEIVED_OBJECT));
            if (step == STEP_6_RESIDENCE)
                countryCode = (int) getArguments().getInt(RECEIVED_OBJECT);
            else
                inputText = (String) getArguments().getString(RECEIVED_OBJECT);
        }
        imageUtils = new Imageutils(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_signup_step, container, false);

        textViewStepLabel = (TextView) view.findViewById(R.id.sign_up_steps);
        imageViewStepRight = (ImageView) view.findViewById(R.id.step_right);
        imageViewStepLeft = (ImageView) view.findViewById(R.id.step_left);


        if (step == STEP_7_FINISH ){
            imageViewStepRight.setVisibility(View.INVISIBLE);
        }


        if (step == STEP_1_FULL_NAME )
            imageViewStepLeft.setVisibility(View.INVISIBLE);

        imageViewStepRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput(step)){
                    if(step == STEP_5_PICTURE)
                        onButtonPressed(STEP_NEXT,inputImage);
                    else if (step == STEP_6_RESIDENCE)
                        onButtonPressed(STEP_NEXT,countryCode);
                    else if (step != STEP_7_FINISH)
                        onButtonPressed(STEP_NEXT,inputText);
                }
            }
        });

        imageViewStepLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed(STEP_BACK,null);
            }
        });

        view = createStepView(view,step);

        return view;
    }

    private View createStepView(View view ,int step) {



        switch(step){
            case STEP_1_FULL_NAME:
                textViewStepLabel.setText("Step 1 from 6");
                editTextInput = (EditText) view.findViewById(R.id.input_full_name);
                editTextInput.setVisibility(View.VISIBLE);
                if(inputText != null)
                    editTextInput.setText(inputText);
                break;

            case STEP_2_USER_NAME:
                textViewStepLabel.setText("Step 2 from 6");
                editTextInput = (EditText) view.findViewById(R.id.input_user_name);
                editTextInput.setVisibility(View.VISIBLE);
                if(inputText != null)
                    editTextInput.setText(inputText);
                break;

            case STEP_3_EMAIL:
                textViewStepLabel.setText("Step 3 from 6");
                editTextInput = (EditText) view.findViewById(R.id.input_email);
                editTextInput.setVisibility(View.VISIBLE);
                if(inputText != null)
                    editTextInput.setText(inputText);
                break;

            case STEP_4_PASSWORD:
                textViewStepLabel.setText("Step 4 from 6");
                editTextInput = (EditText) view.findViewById(R.id.input_password);
                editTextInput2 = (EditText) view.findViewById(R.id.input_re_password);
                editTextInput.setVisibility(View.VISIBLE);
                editTextInput2.setVisibility(View.VISIBLE);
                if(inputText != null) {
                    editTextInput.setText(inputText);
                    editTextInput2.setText(inputText);
                }
                break;

            case STEP_5_PICTURE:
                textViewStepLabel.setText("Step 5 from 6");
                imageViewInput = (ImageView) view.findViewById(R.id.edit_profile_image);
                circleImageView = (de.hdodenhof.circleimageview.CircleImageView) view.findViewById(R.id.user_profile_photo);
                imageViewInput.setVisibility(View.VISIBLE);
                circleImageView.setVisibility(View.VISIBLE);
                if(inputImage != null)
                    circleImageView.setImageBitmap(inputImage);

                imageViewInput.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        imagepicker();
                    }
                });
                break;
            case STEP_6_RESIDENCE:
                textViewStepLabel.setText("\nOne more step...");
                textViewInput = (TextView) view.findViewById(R.id.input_residence);
                textViewInput.setVisibility(View.VISIBLE);
                textViewInput.setText(CountryCodeActivityDialog.getCountryFromCode(countryCode));
                textViewInput.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openListDialog();
                    }
                });
                break;
            case STEP_7_FINISH:
                textViewStepLabel.setText("You are ready to sign up!");
//                textViewConfirmLabel = (TextView) view.findViewById(R.id.input_confirm_code_label);
//                textViewConfirmLabel.setVisibility(View.VISIBLE);
//                editTextInput = (EditText) view.findViewById(R.id.input_confirm_code);
//                editTextInput.setVisibility(View.VISIBLE);
//                editTextInput.addTextChangedListener(new TextWatcher() {
//                    @Override
//                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                    }
//                    @Override
//                    public void onTextChanged(CharSequence s, int start, int before, int count) {
//                        String code = editTextInput.getText().toString();
//                        if (code.isEmpty() || code.length() != 4) {
//                            editTextInput.setError("code with 4 digits only");
//                        } else {
//                            editTextInput.setError(null);
//                            inputText = code;
//                            onButtonPressed(STEP_NEXT,inputText);
//
//                        }
//                    }
//                    @Override
//                    public void afterTextChanged(Editable s) {
//                    }
//                });
                break;
        }
        return view;
    }

    private void openListDialog() {
        final Intent intent = new Intent(getContext(), CountryCodeActivityDialog.class);
        startActivityForResult(intent, ACTION_OPEN);
    }
    private boolean validateInput(int step) {

        boolean  valid = false;

        switch(step){
            case STEP_1_FULL_NAME:

                String name = editTextInput.getText().toString();
                String newInput ="";

                if (name.trim().length() == 0){
                    editTextInput.setError("Enter first and last name");
                    return false;
                }

                String[] inptArray = name.split(" ");
                if (inptArray.length < 2){
                    editTextInput.setError("Enter first and last name");
                    return false;
                }

                for(int i =0 ;i< inptArray.length; i++){
                    char ch = inptArray[i].charAt(0);
                    newInput = newInput + String.valueOf(ch).toUpperCase() +inptArray[i].substring(1) +" ";
                }
                // cutoff the last space
                newInput = newInput.substring(0,newInput.length()-1);
                inputText = newInput;
                valid = true;
                editTextInput.setError(null);

                break;

            case STEP_2_USER_NAME:

                String user = editTextInput.getText().toString();

                if (user.isEmpty() || user.length() < 4 || user.length() > 15) {
                    editTextInput.setError("between 4 and 15 alphanumeric characters");
                    valid = false;
                } else {
                    editTextInput.setError(null);
                    inputText = user;
                    valid = true;
                }

                break;
            case STEP_3_EMAIL:
                String email = editTextInput.getText().toString();
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    editTextInput.setError("enter a valid email address");
                    valid = false;
                } else {
                    editTextInput.setError(null);
                    inputText = email;
                    valid = true;
                }
                // check if email can be genrate
                UuidGenerator uuidGenerator = new UuidGenerator();
                try {
                    UUID uuid = uuidGenerator.GenerateUUIDFromEmail(email);
                } catch (Exception e) {
                    valid = false;
                    editTextInput.setError("this email can't be generate");
                    e.printStackTrace();
                }

                break;

            case STEP_4_PASSWORD:

                String password = editTextInput.getText().toString();
                String password2 = editTextInput2.getText().toString();

                if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
                    editTextInput.setError("between 4 and 10 alphanumeric characters");
                    valid = false;
                } else {
                    editTextInput.setError(null);
                    editTextInput2.setError(null);
                    inputText = password;
                    valid = true;
                }
                if ( !password.equals(password2) ){
                    editTextInput2.setError("passwords are not matching");
                    valid = false;
                }

                break;
            case STEP_5_PICTURE:
                valid = true;
                break;
            case STEP_6_RESIDENCE:
                inputText = CountryCodeActivityDialog.getCountryFromCode(countryCode);
                textViewInput.setText(inputText);
                valid = true;
                break;
            case STEP_7_FINISH:
                valid = true;
                break;
        }

        return valid;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(int answer,Object input) {
        if (mListener != null) {
            mListener.onFragmentInteraction(answer,input);
        }
    }

    @Override
    public void onAttach(Context context) {
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
        void onFragmentInteraction(int answer,Object input);
    }

    public void imagepicker() {

        final CharSequence[] items;

        if(imageUtils.isDeviceSupportCamera()) {
            items=new CharSequence[2];
            items[0]="Camera";
            items[1]="Gallery";
        }
        else {
            items=new CharSequence[1];
            items[0]="Gallery";
        }

        android.app.AlertDialog.Builder alertdialog = new android.app.AlertDialog.Builder(getContext());
        alertdialog.setTitle("Add Image");
        alertdialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Camera")) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null)
                        startActivityForResult(takePictureIntent, CAMERA_REQUEST);

                }
                else if (items[item].equals("Gallery")) {

                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(intent, GALLERY_REQUEST);
                }
            }
        });
        alertdialog.show();
    }

    public Bitmap uriToBitmap(Uri uri){

        Bitmap bitmap = null;
        try {
            bitmap =  MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        imageUtils.request_permission_result(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode,data);

        switch (requestCode) {

            case CAMERA_REQUEST:
                if(resultCode == RESULT_OK) {
                    loadedBitmap = (Bitmap) data.getExtras().get("data");
                    loadedBitmap = ImageConverter.scaleDown(loadedBitmap,100,true);
                    inputImage = loadedBitmap;
                    circleImageView.setImageBitmap(loadedBitmap);

                }
                break;

            case GALLERY_REQUEST:
                if(resultCode== RESULT_OK && data != null) {
                    Log.i("Gallery","Photo");

                    loadedUri = data.getData();
                    loadedBitmap = uriToBitmap(loadedUri);
                    loadedBitmap = ImageConverter.scaleDown(loadedBitmap,100,true);
                    inputImage = loadedBitmap;
                    circleImageView.setImageBitmap(loadedBitmap);

                }
                break;
            case ACTION_OPEN:
                if(resultCode == RESULT_OK) {
                    countryCode = data.getIntExtra(CountryCodeActivityDialog.RESULT_CONTRYCODE, 1);
                    inputText = CountryCodeActivityDialog.getCountryFromCode(countryCode);
                    textViewInput.setText(inputText);
                }
                break;
        }
    }
}
