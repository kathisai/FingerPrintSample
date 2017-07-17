package prathap.fingerprint;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_HOME = 0;
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    @BindView(R.id.input_email)
    EditText _emailText;
    @BindView(R.id.input_password)
    EditText _passwordText;
    @BindView(R.id.btn_login)
    Button _loginButton;
    @BindView(R.id.link_signup)
    TextView _signupLink;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    FingerprintManager fingerprintManager;
    KeyGenerator keyGenerator;
    KeyStore keyStore;
    String KEY_NAME = "Secret Key";
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_activiy);
        ButterKnife.bind(this);
        mToolbar.setTitle("FingerPrint Sample");
        setSupportActionBar(mToolbar);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(RootUtil.isDeviceRooted()){
            // what if device rooted
            // delete the stored content
            deleteInternalFile();

        }
        if (checkFinger()) {
            // We are ready to set up the cipher and the key
            try {
                generateKey();
            } catch (Exception fpe) {
                // Handle exception
            }
        }

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                startActivityForResult(intent, REQUEST_HOME);
            }
        });

        try {
            JSONObject account_details = new JSONObject(readFromFile());
            if (account_details.get("user_name")  == null && account_details.get("pass_word") == null) {
                // some thing wrong with finger print
            } else {
                if (SharedPrefsUtils.getBooleanPreference(LoginActivity.this, Constants.SP_IS_FP_ENABLE, false))
                    showFingerPrintDialog();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void deleteInternalFile() {
        String path = getFilesDir().getAbsolutePath() + "/" + "config.txt";
        File file = new File(path);
                if(file.exists()){
                    file.delete();
                }
    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        // TODO: Implement your own authentication logic here.

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onLoginSuccess or onLoginFailed
                        onLoginSuccess();
                        // onLoginFailed();
                        progressDialog.dismiss();
                    }
                }, 3000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_HOME) {
//            if (resultCode == RESULT_OK) {
            // TODO: Implement successful signup logic here
            // By default we just finish the Activity and log them in automatically
//                this.finish();
            try {
                JSONObject account_details = new JSONObject(readFromFile());
                if (account_details.get("user_name")  == null && account_details.get("pass_word") == null) {
                    // some thing wrong with finger print
                } else {
                    if (SharedPrefsUtils.getBooleanPreference(LoginActivity.this, Constants.SP_IS_FP_ENABLE, false))
                        showFingerPrintDialog();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }



//            if (SharedPrefsUtils.getStringPreference(this, Constants.SP_USER_NAME) == null && SharedPrefsUtils.getStringPreference(this, Constants.SP_PASSWORD_NAME) == null) {
//                // some thing wrong with finger print
//            } else {
//                if (SharedPrefsUtils.getBooleanPreference(LoginActivity.this, Constants.SP_IS_FP_ENABLE, false))
//                    showFingerPrintDialog();
//            }
//            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(true);
        // finish();
        if (!checkFinger()) {
            Toast.makeText(this, "Finger print is not support for your device ", Toast.LENGTH_SHORT).show();
        } else {
            if (!SharedPrefsUtils.getBooleanPreference(LoginActivity.this, Constants.SP_IS_FP_ENABLE, false)) {
                showAlerttoUser();
            } else {
                showFingerPrintDialog();
            }
        }
    }


    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    private void showAlerttoUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        alertDialogBuilder.setTitle("Finger Print Settings");

        alertDialogBuilder
                .setMessage("You can enable finger for simple one touch login ? ")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        //check fingerprint settings and store user name and password only after success auth
                        //enable the finger print flog in Shared preference
                        SharedPrefsUtils.setBooleanPreference(LoginActivity.this, Constants.SP_IS_FP_ENABLE, true);
                        showFingerPrintDialog();

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                        // no need finger print
                        SharedPrefsUtils.setBooleanPreference(LoginActivity.this, Constants.SP_IS_FP_ENABLE, false);
                        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                        startActivityForResult(intent, REQUEST_HOME);
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public boolean isVersionSupport() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private boolean checkFinger() {

        // Keyguard Manager
        KeyguardManager keyguardManager = (KeyguardManager)
                getSystemService(KEYGUARD_SERVICE);

        // Fingerprint Manager
        fingerprintManager = (FingerprintManager)
                getSystemService(FINGERPRINT_SERVICE);

        try {
            // Check if the fingerprint sensor is present
            if (isVersionSupport()) {
                if (!fingerprintManager.isHardwareDetected()) {
                    // Update the UI with a message
                    Toast.makeText(this, "Fingerprint hardware not detected", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                    // If your app doesn't have this permission, then display the following text//
                    Toast.makeText(this, "Please enable the fingerprint permission", Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    Toast.makeText(this,
                            "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                            Toast.LENGTH_LONG).show();
                    return false;
                }

                if (!keyguardManager.isKeyguardSecure()) {
                    Toast.makeText(this,
                            "Secure lock screen hasn't set up.\n"
                                    + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            } else {
                return false;
            }
        } catch (SecurityException se) {
            se.printStackTrace();
        }
        return true;
    }

    private void generateKey() {
        try {
            // Get the reference to the key store
            keyStore = KeyStore.getInstance("AndroidKeyStore");

            // Key generator to generate the key
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,

                    "AndroidKeyStore");

            keyStore.load(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                keyGenerator.init(new
                        KeyGenParameterSpec.Builder(KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT |
                                KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setUserAuthenticationRequired(true)
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build());
            }

            keyGenerator.generateKey();
        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();
        }
    }

    private Cipher generateCipher() {
        try {
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | InvalidKeyException
                | UnrecoverableKeyException
                | KeyStoreException exc) {
            exc.printStackTrace();
            return null;
        }
    }

    public void onAuthenticated() {
        // On success we are storing the user name and password
//        SharedPrefsUtils.setStringPreference(this, Constants.SP_USER_NAME, _emailText.getText().toString());
//        SharedPrefsUtils.setStringPreference(this, Constants.SP_PASSWORD_NAME, _passwordText.getText().toString());
        JSONObject accountObject = new JSONObject();

        try {
            accountObject.put("user_name",_emailText.getText().toString() );
            accountObject.put("pass_word",  _passwordText.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        writeToFile(accountObject.toString());

        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivityForResult(intent, REQUEST_HOME);
    }

    public void showFingerPrintDialog() {
        FingerprintAuthenticationDialogFragment fragment
                = new FingerprintAuthenticationDialogFragment();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fragment.setCryptoObject(new FingerprintManager.CryptoObject(generateCipher()));
        }
        boolean useFingerprintPreference = mSharedPreferences
                .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                        true);
        if (useFingerprintPreference) {
            fragment.setStage(
                    FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
        } else {
            fragment.setStage(
                    FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
        }
        fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
    }


    private void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    private String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
}