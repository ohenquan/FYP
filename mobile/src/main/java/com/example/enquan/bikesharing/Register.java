package com.example.enquan.bikesharing;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Register extends FragmentActivity implements View.OnClickListener {

    private EditText register_name;
    private EditText register_password, register_password_confirm;
    private Button register_button;
    //    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        firebaseAuth = FirebaseAuth.getInstance();
        register_button =(Button)findViewById(R.id.registerpage_button);
        register_name = (EditText)findViewById(R.id.username_register);
        register_password = (EditText)findViewById(R.id.password_register);
        register_password_confirm = (EditText)findViewById(R.id.confirm_password_register);
        register_button.setOnClickListener(this);
    }

    private void register(){
        String loginname = register_name.getText().toString().trim();
        String password  = register_password.getText().toString().trim();
        String c_password  = register_password.getText().toString().trim();
        if(TextUtils.isEmpty(loginname)){
            Toast.makeText(this,"Please enter email",Toast.LENGTH_LONG).show();
            return;
        }

        if(TextUtils.isEmpty(password)){
            Toast.makeText(this,"Please enter password",Toast.LENGTH_LONG).show();
            return;
        }

        if(TextUtils.isEmpty(c_password) || !c_password.equals(password)){
            Toast.makeText(this,"Password is not the same",Toast.LENGTH_LONG).show();
            return;
        }
//        progressDialog.setMessage("Registering Please Wait...");
//        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(loginname, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //checking if success
                        if(task.isSuccessful()){
                            //display some message here
                            Toast.makeText(Register.this,"Successfully registered",Toast.LENGTH_LONG).show();
                            Intent i = new Intent(Register.this,MapsActivity.class);
                            startActivity(i);
                        }else{
                            //display some message here
                            Toast.makeText(Register.this,"Registration Error",Toast.LENGTH_LONG).show();
                        }
//                        progressDialog.dismiss();
                    }
                });

    }

    @Override
    public void onClick(View view) {
        //calling register method on click
        register();
    }
}

