package com.ninhhoa.nguyencse.foody.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.ninhhoa.nguyencse.foody.R;
import com.ninhhoa.nguyencse.foody.custom.ButtonCSE;
import com.ninhhoa.nguyencse.foody.custom.EditTextCSE;
import com.ninhhoa.nguyencse.foody.custom.TextViewCSE;

import java.util.Arrays;

public class AuthActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, FirebaseAuth.AuthStateListener {

    public static int RC_SIGN_IN = 9001;

    private ButtonCSE btnLoginFB;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInOptions gso;
    private GoogleApiClient gac;
    private ButtonCSE btnLoginGoogle;
    private EditTextCSE edtLoginEmail, edtLoginPass;
    private ButtonCSE btnLoginNormal;
    private TextViewCSE txtRegister;
    private ProgressDialog progressDialog;

    // SIGN_IN_METHOD = 0 --> SIGN IN WITH GOOGLE
    // SIGN_IN_METHOD = 1 --> SIGN IN WITH FACEBOOK
    public static int SIGN_IN_METHOD = 0;
    private CallbackManager mCallbackManagerFacebook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        FirebaseAuth.getInstance().signOut();

        edtLoginEmail = (EditTextCSE) findViewById(R.id.edt_login_email);
        edtLoginPass = (EditTextCSE) findViewById(R.id.edt_login_password);
        btnLoginNormal = (ButtonCSE) findViewById(R.id.btn_login_normal);
        btnLoginFB = (ButtonCSE) findViewById(R.id.btn_login_facebook);
        btnLoginGoogle = (ButtonCSE) findViewById(R.id.btn_login_google);
        txtRegister = (TextViewCSE) findViewById(R.id.txt_register);

        progressDialog = new ProgressDialog(this, R.style.LoadingCircle);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);

        mCallbackManagerFacebook = CallbackManager.Factory.create();
        firebaseAuth = FirebaseAuth.getInstance();
        createClientGoogle();

        btnLoginGoogle.setOnClickListener(this);
        btnLoginFB.setOnClickListener(this);
        btnLoginNormal.setOnClickListener(this);
        txtRegister.setOnClickListener(this);
    }

    private void createClientGoogle() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        gac = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void loginWithGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(gac);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void credentialAuth(String tokenID) {
        AuthCredential authCredential = null;
        if (SIGN_IN_METHOD == 0) {
            authCredential = GoogleAuthProvider.getCredential(tokenID, null);
        } else if (SIGN_IN_METHOD == 1) {
            authCredential = FacebookAuthProvider.getCredential(tokenID);
        }
        if (authCredential != null) {
            firebaseAuth.signInWithCredential(authCredential);
        }
    }

    private void loginWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        LoginManager.getInstance().registerCallback(mCallbackManagerFacebook,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        String tokenID = loginResult.getAccessToken().getToken();
                        SIGN_IN_METHOD = 1;
                        credentialAuth(tokenID);
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onError(FacebookException exception) {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                if (account != null) {
                    credentialAuth(account.getIdToken());
                }
            }
        } else {
            mCallbackManagerFacebook.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loginWithEmailAndPassword(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();

                        if (!task.isSuccessful()) {
                            Toast.makeText(AuthActivity.this, R.string.login_failed,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(AuthActivity.this, R.string.login_failed,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_login_normal:
                if (TextUtils.isEmpty(edtLoginEmail.getText().toString())) {
                    edtLoginEmail.setError(getString(R.string.email_must_not_be_empty));
                } else if (TextUtils.isEmpty(edtLoginPass.getText().toString())) {
                    edtLoginPass.setError(getString(R.string.password_must_not_be_empty));
                } else {
                    progressDialog.show();
                    loginWithEmailAndPassword(edtLoginEmail.getText().toString(), edtLoginPass.getText().toString());
                }
                break;
            case R.id.btn_login_google:
                loginWithGoogle();
                break;
            case R.id.btn_login_facebook:
                loginWithFacebook();
                break;
            case R.id.txt_register:
                startActivity(new Intent(AuthActivity.this, RegisterActivity.class));
                break;
        }
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_LONG).show();
            Intent intentHome = new Intent(AuthActivity.this, HomeActivity.class);
            startActivity(intentHome);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(this);
    }
}
