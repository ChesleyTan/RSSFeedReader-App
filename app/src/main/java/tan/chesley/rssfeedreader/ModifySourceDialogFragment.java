package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
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
        final SourcesOpenHelper dbHelper = new SourcesOpenHelper(getActivity());
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialogInterface, int i) {
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            dbHelper.deleteSource(source);
                            mCallback.onModifySourceCallback();
                        }
                    }
                };
                builder.setMessage(getResources().getString(R.string.confirm_deleteSource))
                       .setPositiveButton(getResources().getString(R.string.yes), onClickListener)
                       .setNegativeButton(getResources().getString(R.string.cancel), onClickListener)
                       .show();
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
