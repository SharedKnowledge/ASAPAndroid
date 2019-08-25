package net.sharksystem.asap.android;

import android.content.Intent;

public interface ASAPActivityHelperUserMustDelegate {
    void onActivityResult(int requestCode, int resultCode, Intent data);

    // life cycle
    void onStart();
    void onStop();
    void onDestroy();
}
