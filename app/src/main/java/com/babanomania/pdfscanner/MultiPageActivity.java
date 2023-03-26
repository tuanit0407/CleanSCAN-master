package com.babanomania.pdfscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.babanomania.pdfscanner.utils.FileIOUtils;
import com.scanlibrary.ScanActivity;
import com.scanlibrary.ScanConstants;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MultiPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_page);

        setTitle( getResources().getString(R.string.multi_page_title) );

        final String stagingDirPath =  getApplicationContext().getString( R.string.base_staging_path);
        final List<File> stagingFiles = FileIOUtils.getAllFiles( stagingDirPath );

        final GridView pagesGridView = (GridView) findViewById(R.id.multi_page_grid);
        final BaseAdapter gvAdapter = new ImageAdapterGridView(this);
        pagesGridView.setAdapter(gvAdapter);

        pagesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if(position == 0) scanMore(view);
                else showImage(stagingFiles, position, view);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Intent out = new Intent();
        out.putExtra(ScanConstants.SCANNED_RESULT, data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT));
        out.putExtra(ScanConstants.SCAN_MORE, data.getExtras().getBoolean(ScanConstants.SCAN_MORE));
        setResult(RESULT_OK, out);
        finish();

        System.gc();
    }

    public void saveNow(View view) {
        Intent out = new Intent();
        out.putExtra(ScanConstants.SAVE_PDF, Boolean.TRUE);
        setResult(RESULT_OK, out);
        finish();
    }

    public void scanMore(View view) {
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, ScanConstants.OPEN_CAMERA);

        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        startActivityForResult(intent, ScanConstants.START_CAMERA_REQUEST_CODE, options.toBundle());
    }

    public void showImage(List<File> stagingFiles, int position, View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String newFileName = stagingFiles.get(position - 1).getPath();
        File toOpen = new File( newFileName );

        Uri sharedFileUri = FileProvider.getUriForFile(view.getContext(), "com.babanomania.pdfscanner.provider", toOpen);
        intent.setDataAndType( sharedFileUri, "image/png");
        PackageManager pm = view.getContext().getPackageManager();

        if (intent.resolveActivity(pm) != null) {
            view.getContext().startActivity(intent);
        }
    }

    public class ImageAdapterGridView extends BaseAdapter {
        private Context mContext;
        private GridView mGridView;
        private List<File> stagingFiles;

        public ImageAdapterGridView(Context c) {
            mContext = c;

            final String stagingDirPath =  getApplicationContext().getString( R.string.base_staging_path);
            stagingFiles = FileIOUtils.getAllFiles( stagingDirPath );

            registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    stagingFiles = FileIOUtils.getAllFiles( stagingDirPath );
                }
            });
        }

        public int getCount() {
            return stagingFiles.size() + 1;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return Long.valueOf(position);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            int width = (displayMetrics.widthPixels - 100 )/ 3;
            int height = ( width / 3 ) * 4;

            if ( position == 0 ){

                View addMoreView = getLayoutInflater().inflate(R.layout.add_more_img, null);
                addMoreView.setLayoutParams(new GridView.LayoutParams(width, height));
                return addMoreView;

            }else {

                View eachFileView = getLayoutInflater().inflate(R.layout.each_file_img, null);
                eachFileView.setLayoutParams(new GridView.LayoutParams( width, height ));

                ImageView deleteButton = eachFileView.findViewById(R.id.each_file_delete);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        stagingFiles.get( stagingFiles.size() - position ).delete();
                        notifyDataSetChanged();
                    }
                });

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 3;

                File imgFile = stagingFiles.get( stagingFiles.size() - position );
                Bitmap myBitmap = BitmapFactory.decodeFile( imgFile.getAbsolutePath(), options );

                ImageView imageView = eachFileView.findViewById(R.id.each_file_screenshot);
                imageView.setImageBitmap(myBitmap);

                TextView textView = eachFileView.findViewById(R.id.each_pageno);
                textView.setText("Page " + (stagingFiles.size() - position  + 1 ) );

                return eachFileView;
            }
        }
    }
}