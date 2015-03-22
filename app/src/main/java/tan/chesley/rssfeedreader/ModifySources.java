package tan.chesley.rssfeedreader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ModifySources extends ListActivity implements ModifySourceDialogFragment.ModifySourceCallbacks, AddSourceDialogFragment.AddSourceCallbacks{

    private static final String TAG_SOURCES = "tan.chesley.rssfeedreader.sources";
    private static final String TAG_MODIFY_SOURCE_DIALOG_FRAGMENT = "tan.chesley.rssfeedreader.modifysourcedialogfragment";
    private static final String TAG_ADD_SOURCE_DIALOG_FRAGMENT = "tan.chesley.rssfeedreader.addsourcedialogfragment";
    private static final int TAG_IMPORT_FILE_RESULT_CODE = 467678; // "import" in phone number representation
    private static final int TAG_EXPORT_FILE_RESULT_CODE = 397678; // "export" in phone number representation
    private final Activity activity = this;
    private ArrayList<String> sources;
    private boolean isAutomaticChange;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrightnessControl.toggleBrightness(getApplicationContext(), this);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                                      | ActionBar.DISPLAY_USE_LOGO
                                      | ActionBar.DISPLAY_SHOW_TITLE
                                      | ActionBar.DISPLAY_HOME_AS_UP);
        }
        Utils.setActivityTitle(this, getResources().getString(R.string.sources));
        setContentView(R.layout.modify_sources);
        if (savedInstanceState != null) {
            sources = savedInstanceState.getStringArrayList(TAG_SOURCES);
        }
        else {
            SourcesOpenHelper sourceDbHelper = new SourcesOpenHelper(this);
            sources = sourceDbHelper.getSourcesArrayList();
        }
        showListView(false);
    }

    @Override
    protected void onListItemClick (ListView l, View v, final int position, long id) {
        ModifySourceDialogFragment dialog = new ModifySourceDialogFragment();
        Bundle args = new Bundle();
        args.putString(ModifySourceDialogFragment.TAG_SOURCE, sources.get(position));
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), TAG_MODIFY_SOURCE_DIALOG_FRAGMENT);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(TAG_SOURCES, sources);
    }

    private class SourcesAdapter extends ArrayAdapter<String> {
        public SourcesAdapter(ListActivity context) {
            super(context, R.layout.sources_list_item, sources);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                    R.layout.sources_list_item, parent, false);
            }
            TextView sourceTextView = (TextView) convertView.findViewById(R.id.sourceTextView);
            TextView positionTextView = (TextView) convertView.findViewById(R.id.positionTextView);
            ToggleButton disabledToggleButton = (ToggleButton) convertView.findViewById(R.id.disabledToggleButton);
            disabledToggleButton.setOnCheckedChangeListener(new ToggleButtonOnCheckedChangeListener(sourceTextView));
            SourcesOpenHelper dbHelper = new SourcesOpenHelper(activity);
            String source = getItem(position);
            // Check if the source is disabled and change its appearance as appropriate
            if (!dbHelper.isEnabled(source)) {
                sourceTextView.setTextColor(getResources().getColor(R.color.DisabledTextColor));
                isAutomaticChange = true;
                disabledToggleButton.setChecked(false);
                isAutomaticChange = false;
            }
            else {
                sourceTextView.setTextColor(getResources().getColor(R.color.AppPrimaryTextColor));
                isAutomaticChange = true;
                disabledToggleButton.setChecked(true);
                isAutomaticChange = false;
            }
            sourceTextView.setText(source);
            positionTextView.setText(Integer.toString(position + 1)); // Add 1 for One-based indexing
            return convertView;
        }
    }

    public class ToggleButtonOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        private TextView sourceTextView;
        public ToggleButtonOnCheckedChangeListener(TextView sourceTextView) {
            this.sourceTextView = sourceTextView;
        }
        @Override
        public void onCheckedChanged (CompoundButton compoundButton, boolean b) {
            if (b) {
                sourceTextView.setTextColor(getResources().getColor(R.color.AppPrimaryTextColor));
            }
            else {
                sourceTextView.setTextColor(getResources().getColor(R.color.DisabledTextColor));
            }
            if (!isAutomaticChange) {
                SourcesOpenHelper dbHelper = new SourcesOpenHelper(activity);
                dbHelper.setEnabled(sourceTextView.getText().toString(), b);
            }
        }
    }

    public void showListView(boolean updateSources) {
        if (updateSources) {
            SourcesOpenHelper dbHelper = new SourcesOpenHelper(this);
            sources = dbHelper.getSourcesArrayList();
        }
        getListView().setAdapter(new SourcesAdapter(this));
    }

    public void onModifySourceCallback() {
        showListView(true);
    }

    public void onAddSourceCallback() {
        showListView(true);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.modifysources, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected (int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            AddSourceDialogFragment dialog = new AddSourceDialogFragment();
            dialog.show(getFragmentManager(), TAG_ADD_SOURCE_DIALOG_FRAGMENT);
            return true;
        }
        else if (id == R.id.action_import) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("file/*");
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            if (activities.size() > 0) {
                startActivityForResult(intent, TAG_IMPORT_FILE_RESULT_CODE);
            } else {
                Toaster.showToast(this, getResources().getString(R.string.importFailedNoFileBrowser), Toast.LENGTH_LONG);
            }
            return true;
        }
        else if (id == R.id.action_export) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File sdCard = Environment.getExternalStorageDirectory();
                File path = new File(sdCard.getAbsolutePath() + "/tan.chesley.rssfeedreader/feeds");
                path.mkdirs();
                File file = new File(path, "feeds-export.txt");
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    String[] sources = new SourcesOpenHelper(this).getSources();
                    writer.write("tan.chesley.rssfeedreader.export\n");
                    for (String s : sources) {
                        writer.write(s + "\n");
                    }
                    writer.close();
                    Toaster.showToast(this, getResources().getString(R.string.successfulExportToFile) + file.getAbsolutePath(), Toast.LENGTH_LONG);
                } catch (IOException e) {
                    Toaster.showToast(this, getResources().getString(R.string.errorOccurredDuringWrite), Toast.LENGTH_LONG);
                    e.printStackTrace();
                }
            }
            else {
                Toaster.showToast(this, getResources().getString(R.string.noExternalStorageAccess), Toast.LENGTH_LONG);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == TAG_IMPORT_FILE_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                String filePath = uri.getPath();
                Log.e("Import", "File chosen: " + filePath);
                try {
                    BufferedReader reader;
                    if (uri.getScheme().equals("content")) {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        reader = new BufferedReader(new InputStreamReader(inputStream));
                    }
                    else {
                        File file = new File(filePath);
                        reader = new BufferedReader(new FileReader(file));
                    }
                    String line = reader.readLine();
                    if (line != null && line.equals("tan.chesley.rssfeedreader.export")) {
                        SourcesOpenHelper dbHelper = new SourcesOpenHelper(this);
                        while ((line = reader.readLine()) != null) {
                            dbHelper.addSource(line, 1);
                        }
                        Toaster.showToast(this, getResources().getString(R.string.successfulImportFromFile) + filePath, Toast.LENGTH_LONG);
                        // Reload ListView items
                        showListView(true);
                    }
                    else {
                        Log.e("File format not recognized: ", line);
                        Toaster.showToast(this, getResources().getString(R.string.invalidFileFormat), Toast.LENGTH_LONG);
                    }
                    reader.close();
                    // TODO validate sources that are imported
                } catch (IOException e) {
                    Toaster.showToast(this, getResources().getString(R.string.errorOccurredDuringRead), Toast.LENGTH_LONG);
                    e.printStackTrace();
                }
            }
        }
    }

    public void validateAndAddSource(String s) {
        ValidationAsyncTask task = new ValidationAsyncTask();
        task.execute(s);
    }

    private class ValidationAsyncTask extends AsyncTask<String, Void, Void> {

        final long validationTimeout = 3000;
        final long startTime = System.currentTimeMillis();
        private String s = "";
        private boolean isValidFeed = true;

        @Override
        protected Void doInBackground (String... strings) {
            s = strings[0];
            SAXParserFactory mySAXParserFactory = SAXParserFactory
                .newInstance();
            SAXParser mySAXParser = null;
            XMLReader myXMLReader = null;
            try {
                mySAXParser = mySAXParserFactory.newSAXParser();
                myXMLReader = mySAXParser.getXMLReader();
            } catch (SAXException e) {
                e.printStackTrace();
                Log.e("Validation", "Could not connect to RSS feed! Error 1");
                isValidFeed = false;
                return null;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                Log.e("Validation", "Could not connect to RSS feed! Error 2.");
                isValidFeed = false;
                return null;
            }
            if (System.currentTimeMillis() - startTime > validationTimeout) {
                Log.e("Validation", "Validation timeout reached.");
                isValidFeed = false;
                return null;
            }
            DefaultHandler myRSSValidationHandler = new DefaultHandler();
            myXMLReader.setContentHandler(myRSSValidationHandler);
            InputSource myInputSource;
            InputStream feedStream;
            URL url = null;
            try {
                url = new URL(s);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.e("Validation", "Cannot connect to RSS feed! Error 3.");
                isValidFeed = false;
                return null;
            }
            if (System.currentTimeMillis() - startTime > validationTimeout) {
                Log.e("Validation", "Validation timeout reached.");
                isValidFeed = false;
                return null;
            }
            try {
                feedStream = url.openStream();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Validation", "Cannot connect to RSS feed! Error 4.");
                isValidFeed = false;
                return null;
            }
            if (System.currentTimeMillis() - startTime > validationTimeout) {
                Log.e("Validation", "Validation timeout reached.");
                isValidFeed = false;
                return null;
            }
            myInputSource = new InputSource(feedStream);
            try {
                myXMLReader.parse(myInputSource);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Validation", "Cannot connect to RSS feed! Error 5.");
                isValidFeed = false;
                return null;
            } catch (SAXException e) {
                e.printStackTrace();
                Log.e("Validation", "Cannot connect to RSS feed! Error 6.");
                isValidFeed = false;
                return null;
            }
            return null;
        }


        @Override
        protected void onPreExecute () {
            super.onPreExecute();
            Toaster.showToast(activity, getResources().getString(R.string.validatingNewSource), Toast.LENGTH_SHORT);
        }

        @Override
        protected void onPostExecute (Void aVoid) {
            super.onPostExecute(aVoid);
            if (isValidFeed) {
                SourcesOpenHelper dbHelper = new SourcesOpenHelper(getApplicationContext());
                dbHelper.addSource(s, SourcesOpenHelper.ENABLED);
                Toaster.showToast(activity, getResources().getString(R.string.validSource), Toast.LENGTH_SHORT);
                showListView(true);
            }
            else {
                Log.e("Validation", s + " is an invalid source.");
                Toaster.showToast(activity, getResources().getString(R.string.invalidSource), Toast.LENGTH_LONG);
            }
        }
    }
}
