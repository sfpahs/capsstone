package com.example.pre_capstone
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val navHostController = LocalNavController.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xfffafafa)),

    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center){
            Text("로그인", style = MaterialTheme.typography.h5)

            Spacer(modifier = Modifier.height(32.dp))

            // 이메일 입력창
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),

            )

            Spacer(modifier = Modifier.height(32.dp))

            // 비밀번호 입력창
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 로그인 버튼
            Button(
                onClick = {
                signInWithEmailAndPassword(email = email, password = password,
                    onSuccess = {
                        navHostController.navigate("main")


                    },
                    onError = {error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    })
                },
                colors = ButtonDefaults.buttonColors(

                    backgroundColor = colorResource(R.color.myButtonColor)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("로그인", fontSize = 20.sp, color = colorResource(R.color.myBlack))
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    navHostController.navigate("signUp")

                },
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(R.color.myButtonColor)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("회원가입", fontSize = 20.sp, color = colorResource(R.color.myBlack))
            }
        }
    }
}

@Composable
fun signupPage() {
    var userName by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val navHostController = LocalNavController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            //todo 아이콘으로 변경할것
            Text(text = "회원가입",
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                fontSize = 20.sp)
            Spacer(modifier = Modifier.height(15.dp))

            TextField(
                value = userName,
                singleLine = true,
                onValueChange = {
                    userName = it

                },
                label = { Text("유저이름") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = MaterialTheme.shapes.medium)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = userId,
                singleLine = true,
                onValueChange = {
                    userId = it

                },
                label = { Text("이메일") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = MaterialTheme.shapes.medium)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text("비밀번호는 6자리이상으로 해주세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = MaterialTheme.shapes.medium)
                    .padding(8.dp)

            )

        }

        Button(
            //todo 자동 저장되기에 로그인하고 바로 메인화면이동
            onClick = {
                if(!isPasswordCorrect(password)) Toast.makeText(context, "비밀번호 6자리 이상 설정해주세요", Toast.LENGTH_SHORT).show()
                else
                    checkEmailExists(email = userId){ exist ->
                        if(exist){
                            Toast.makeText(context, "이미 존재하는 이메일입니다", Toast.LENGTH_SHORT).show()
                        }
                        else{
                            signUp(email = userId, password = password, name = userName, context = context,
                                onSuccessCallback = {navHostController.navigate("main")},
                                onFailCallback = { e ->
                                    Log.e("signUpError", "SignupPage: ${e.exception}")

                                }
                            )

                        }
                    }
                      },
            //todo 회원가입 활성화 로직 변경할것
            enabled = userName.isNotBlank() && userId.isNotBlank()  && isPasswordCorrect(password),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = MaterialTheme.shapes.medium)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (userName.isNotBlank() && userId.isNotBlank()  && isPasswordCorrect(password)) colorResource(R.color.myButtonColor) else colorResource(R.color.myGray)
            )
        ) {
            Text("회원가입", color = Color.White, fontSize = 20.sp)
        }
    }

}

// 비밀번호 확인 함수 (예시)
fun isPasswordCorrect(password: String): Boolean {
    // 실제 구현에서는 서버 통신 등을 통해 확인
    return password.length >= 6
}
