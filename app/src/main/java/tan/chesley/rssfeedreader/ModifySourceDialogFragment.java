package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ModifySourceDialogFragment extends DialogFragment{
    public static final String TAG_SOURCE = "tan.chesley.rssfeedreader.source";
    private String source;
    private ModifySourceCallbacks mCallback;

    interface ModifySourceCallbacks {
        void onModifySourceCallback();
    }

    @Override
    public void onAttach (Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof ModifySourceCallbacks)) {
            throw new ClassCastException(activity.toString() + " must implement ModifySourceCallbacks");
        }
        else {
            mCallback = (ModifySourceCallbacks) activity;
        }
    }

    @Override
    public Dialog onCreateDialog (Bundle savedInstanceState) {
        source = getArguments().getString(TAG_SOURCE);
        final Dialog dialog = new Dialog(getActivity(), R.style.SemiTransparentDialog);
        dialog.setContentView(R.layout.modify_source_dialog);
        dialog.setTitle(getResources().getString(R.string.editSource));
        final EditText sourceEditText = (EditText) dialog.findViewById(R.id.sourceEditText);
        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        Button deleteButton = (Button) dialog.findViewById(R.id.deleteButton);
        sourceEditText.setText(source);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
                SourcesOpenHelper dbHelper = new SourcesOpenHelper(getActivity());
                dbHelper.deleteSource(source);
                String s = sourceEditText.getText().toString();
                if (!s.startsWith("http://") && !s.startsWith("https://")) {
                    s = "http://" + s;
                    Log.e("URL", "URL modified to " + s);
                }
                ((ModifySources)getActivity()).validateAndAddSource(s);
                dialog.dismiss();
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View view) {
                SourcesOpenHelper dbHelper = new SourcesOpenHelper(getActivity());
                dbHelper.deleteSource(source);
                mCallback.onModifySourceCallback();
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
