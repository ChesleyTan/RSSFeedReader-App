package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
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
        addSourceDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
                SourcesManager sm = new SourcesManager(getActivity());
                // TODO validation and validation feedback text
                sm.addSource(addSourceDialogEditText.getText().toString());
                mCallback.onAddSourceCallback();
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
