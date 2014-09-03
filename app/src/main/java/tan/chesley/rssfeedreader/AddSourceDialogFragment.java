package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AddSourceDialogFragment extends DialogFragment{

    private AddSourceCallbacks mCallback;

    interface AddSourceCallbacks {
        void onAddSourceCallback();
    }

    @Override
    public void onAttach (Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AddSourceCallbacks)) {
            throw new ClassCastException(activity.toString() + " must implement AddSourceCallbacks");
        }
        else {
            mCallback = (AddSourceCallbacks) activity;
        }
    }

    @Override
    public Dialog onCreateDialog (Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity(), R.style.SemiTransparentDialog);
        dialog.setContentView(R.layout.add_source_dialog);
        dialog.setTitle(getResources().getString(R.string.addSourceTitle));
        final EditText addSourceDialogEditText = (EditText) dialog.findViewById(R.id.addSourceDialogEditText);
        final Button addSourceDialogButton = (Button) dialog.findViewById(R.id.addSourceDialogButton);
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager.hasPrimaryClip()) {
            if (clipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                String pasteUrl = clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
                if (pasteUrl != null && pasteUrl.startsWith("http")) {
                    addSourceDialogEditText.setText(pasteUrl);
                }
            }
        }
        addSourceDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
                String s = addSourceDialogEditText.getText().toString();
                if (!s.startsWith("http://") && !s.startsWith("https://")) {
                    s = "http://" + s;
                    Log.e("URL", "URL modified to " + s);
                }
                ((ModifySources)getActivity()).validateAndAddSource(s);
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
