package com.example.takephoto;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;




public class MainActivity extends AppCompatActivity {
    private ImageView mimageView;
    private MnistClassifier mnistClassifier;
    private TextView output;

    private static final int REQUEST_IMAGE_CAPTURE=101;
    private ImageView targetImage;


    int PICK_IMAGE_MULTIPLE = 1;
    String imageEncoded;
    List<String> imagesEncodedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadMnistClassifier();
        mimageView = findViewById(R.id.imageView);
        output = findViewById(R.id.textView);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);


    }
    public void sort(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), PICK_IMAGE_MULTIPLE);
    }


    public void FileSelect(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);
    }

    public void TakeThePic(View view) {
        Intent imageTakeintent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(imageTakeintent.resolveActivity(getPackageManager() )!= null){
            startActivityForResult(imageTakeintent,REQUEST_IMAGE_CAPTURE);
        }

    }
    private void loadMnistClassifier() {
        try {
            mnistClassifier = MnistClassifier.classifier(getAssets(), MnistModelConfig.MODEL_FILENAME);
            System.out.println("DONE LOADING MODEL");
        } catch (IOException e) {
            Toast.makeText(this, "MNIST model couldn't be loaded. Check logs for details.", Toast.LENGTH_SHORT).show();
            System.out.println("THERE IS A PROBLEM IN MODEL");
//            e.printStackTrace();
        }
    }
    protected Bitmap req_101(int requestCode ,int resultCode, Intent data ){
        Bitmap bitmap =null;
        System.out.println(requestCode);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            bitmap = (Bitmap) extras.get("data");

        }
        return bitmap;
    }

    protected Bitmap req_0(int requestCode ,int resultCode, Intent data ){
        Bitmap bitmap =null;
        System.out.println(requestCode);
        if (resultCode == RESULT_OK){
            Uri targetUri = data.getData();

            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
//                    mimageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return bitmap;
    }
    private void SaveImage(Bitmap bitmap, int i, String dirName) throws IOException {
        File path = Environment.getExternalStorageDirectory();
        File dir = new File(path+"/"+dirName+"/");
        dir.mkdir();
        File file = new File(dir,i+".jpg");
        OutputStream out =null;
        out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
        out.flush();
        out.close();
    }


    protected void req_1(int requestCode ,int resultCode, Intent data ) throws IOException {
        Bitmap bitmap =null;
        int count = 0;
        System.out.println(requestCode);
        if(resultCode == RESULT_OK) {
            if(data.getClipData() != null) {
                count = data.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                for(int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    System.out.println(i);
                    try {
                        bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        Bitmap squareBitmap = ThumbnailUtils.extractThumbnail(bitmap, getScreenWidth(), getScreenWidth());
                        Bitmap preprocessedImage = ImageUtils.prepareImageForClassification(squareBitmap);
                        ArrayList<Classification> recognitions = (ArrayList<Classification>) mnistClassifier.recognizeImage(preprocessedImage);
                        String max =null ;
                        Float conf_max=0f;
                        for(Classification elements : recognitions){
                            if(conf_max<= elements.confidence*100f){
                                conf_max = elements.confidence*100F;
                                max = elements.title;

                            }

                        }
                        SaveImage(bitmap,i,max);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
                //do something with the image (save it to some directory or whatever you need to do with it here)
            }
        } else if(data.getData() != null) {
            String imagePath = data.getData().getPath();
            //do something with the image (save it to some directory or whatever you need to do with it here)
        }
    output.setText("Done Processing whole folder \n"+"Total images    "+count);

    }

    /*
    The below method loads the image and classification model for all the intent
     */

    @Override
    protected void onActivityResult(int requestCode ,int resultCode, Intent data) {
        Bitmap bitmap = null;
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 101){
            bitmap = req_101(requestCode,resultCode,data);
        }

        else if(requestCode == 0){
//            super.onActivityResult(requestCode, resultCode, data);
            bitmap = req_0(requestCode,resultCode,data);
        }
        else if(requestCode == 1){
            try {
                req_1(requestCode,resultCode,data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == 101 || requestCode == 0) {
            Bitmap squareBitmap = ThumbnailUtils.extractThumbnail(bitmap, getScreenWidth(), getScreenWidth());
            Bitmap preprocessedImage = ImageUtils.prepareImageForClassification(squareBitmap);
            mimageView.setImageBitmap(preprocessedImage);
            ArrayList<Classification> recognitions = (ArrayList<Classification>) mnistClassifier.recognizeImage(preprocessedImage);
            System.out.println(recognitions);
            Toast.makeText(this, recognitions.toString(), Toast.LENGTH_LONG).show();
            output.setText(recognitions.toString());

        }
    }

    private int getScreenWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }



}