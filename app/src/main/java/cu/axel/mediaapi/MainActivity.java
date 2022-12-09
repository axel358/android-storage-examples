package cu.axel.mediaapi;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.widget.Toast;
import android.os.Environment;
import androidx.documentfile.provider.DocumentFile;
import android.provider.DocumentsContract;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity
{
    private final int OPEN_REQUEST_CODE=4;
    private final int OPEN_FOLDER_REQUEST_CODE=6;
    private final int GET_CONTENT_REQUEST_CODE=3;
    private final int SAVE_REQUEST_CODE=5;
    private String name;
    private TextView openUriTv,folderUriTv;
    private Uri openUri,saveUri,folderUri;
    private ContentResolver resolver;

    private SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        openUriTv = findViewById(R.id.mainTextView1);
        folderUriTv = findViewById(R.id.folder_uri_tv);
        resolver = getContentResolver();
        name = "My Video.mp4";

        if (Build.VERSION.SDK_INT > 22 && Build.VERSION.SDK_INT < 30)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 156);
            }
        }
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        folderUri = Uri.parse(sp.getString("folder_uri", ""));
        folderUriTv.setText("Folder uri: " + folderUri.toString());
    }
    public void openDocument(View v)
    {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("video/*"), OPEN_REQUEST_CODE);
    }

    public void openDocumentTree(View v)
    {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), OPEN_FOLDER_REQUEST_CODE);
    }

    public void getContent(View v)
    {
        startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("video/*"), GET_CONTENT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == OPEN_REQUEST_CODE)
            {
                openUri = data.getData();
                resolver.takePersistableUriPermission(openUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                openUriTv.setText("Document uri: " + openUri);
            }
            else if (requestCode == GET_CONTENT_REQUEST_CODE)
            {
                openUri = data.getData();
                openUriTv.setText("Document uri: " + openUri);
            }

            else if (requestCode == SAVE_REQUEST_CODE)
            {
                copy(openUri, data.getData(), false);
            }
            else if (requestCode == OPEN_FOLDER_REQUEST_CODE)
            {
                folderUri = data.getData();
                resolver.takePersistableUriPermission(folderUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                sp.edit().putString("folder_uri", folderUri.toString()).commit();
                folderUriTv.setText("Folder uri: " + folderUri.toString());
            }
        }
    }

    public void saveDocument(View v)
    {
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("video/*").putExtra(Intent.EXTRA_TITLE, "My video.mp4"), SAVE_REQUEST_CODE);
    }

    public void saveMedia(View v)
    {
        Uri videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, name);
        if (Build.VERSION.SDK_INT > 29)
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/My Videos/");
        saveUri = resolver.insert(videoCollection, values);

        copy(openUri, saveUri, true);

    }

    public void saveToFolder(View v)
    {
        DocumentFile dir = DocumentFile.fromTreeUri(this, folderUri);

        DocumentFile myDir = dir.findFile("My Videos");

        if (myDir == null)
        {
            myDir = dir.createDirectory("My Videos");
        }

        DocumentFile file = myDir.createFile("video/*", "My Video.mp4");

        copy(openUri, file.getUri(), false);
    }

    public void copy(Uri in, Uri out, boolean update)
    {

        try
        {
            InputStream is= resolver.openInputStream(in);
            OutputStream os=resolver.openOutputStream(out);
            byte buffer[] = new byte[1024];
            try
            {
                while (is.read(buffer) != -1)
                {
                    os.write(buffer);   
                }
                is.close();
                os.close();
                Toast.makeText(this, "Saved", 5000).show();
                if (update)
                {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Video.Media.DISPLAY_NAME, name);
                    resolver.update(saveUri, values, null, null);
                }
            }
            catch (IOException e)
            {
                Toast.makeText(this, "Could not read/write stream \n" + e.toString() + "\n" + e.getMessage(), 10000).show();  
            }

        }
        catch (FileNotFoundException e)
        {
            Toast.makeText(this, "Could not open stream \n" + e.toString() + "\n" + e.getMessage(), 10000).show();
        }

    }



}
