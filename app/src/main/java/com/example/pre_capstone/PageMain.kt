import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.pre_capstone.TimerService
import com.example.pre_capstone.loadUserName
import com.example.pre_capstone.logOut
import com.example.pre_capstone.model.TimerSetting
import com.example.pre_capstone.model.TimerViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview
@Composable
fun MainPage(navHostController: NavHostController = rememberNavController(), viewModel: TimerViewModel = viewModel()) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val timerSettings by viewModel.timerSettings.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var userName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(10.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center

        ) {
            items(timerSettings.size) { index ->
                val setting = timerSettings[index]
                myBox(
                    modifier = Modifier.aspectRatio(1f),

                    navHostController = navHostController,
                    timerSetting = setting
                )
            }
            //todo나중에 편집아이콘 만들고 넣기 - 최대개수랑 편집아이콘이랑 합쳐야함
//            item {
//                addBox(
//                    modifier = Modifier.aspectRatio(1f),
//                    color = Color.DarkGray,
//                    text = "+",
//                    category = timerSettings.size + 1,
//                    navHostController = navHostController
//                )
//            }
        }

    }
    Row (modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween){
        Box{

            if (user != null) {

                loadUserName(uid = user.uid, onComplete = {name -> userName = name!! })
                if(!userName.equals("")) Text(text = userName + " 님")
            }


        }
        Log.i("main", "MainPage: ${userName}")
        Button(
            onClick = { logOut(context = context)
            navHostController.navigate("signin")}
        ) { Text(text = "Log out")}
    }

    user?.let {
        scope.launch {
            viewModel.loadTimerSettings(it.uid)
            loadUserName(it.uid) { name ->
                userName = name ?: ""
            }
            delay(1000)
            isLoading = false
        }

    }

    LoadingScreen(isLoading = isLoading)
}

@Composable
fun myBox(modifier: Modifier, navHostController: NavHostController, timerSetting: TimerSetting){
    val navController = rememberNavController()
    Column (modifier = modifier
        .fillMaxHeight(1f)
        .padding(10.dp)
        .background(
            color = Color(timerSetting.backgroundColor),
            shape = RoundedCornerShape(20.dp)
        )
        .clickable {
            TimerService.maxRestTime = timerSetting.restTime
            TimerService.maxWorkingTime = timerSetting.workTime

            val route ="timer/${timerSetting.name}/${timerSetting.workTime}/${timerSetting.restTime}/${timerSetting.index}"
            navHostController.navigate(route) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
        ){ Text(text = timerSetting.name,  fontSize = 25.sp)
        Text(text = "work: ${timerSetting.workTime/60}분", color = Color.White)
        Text(text = "rest: ${timerSetting.restTime/60}분", color = Color.White)}
}
@Composable
fun addBox(modifier: Modifier, color : Color,text : String, category : Int, navHostController: NavHostController){
    Column (modifier = modifier
        .fillMaxHeight(1f)
        .padding(10.dp)
        .background(
            color = color,
            shape = RoundedCornerShape(20.dp)
        )
        .clickable {
            //todo 편집창으로 연결할 것
            navHostController.navigate("timer${category}")
        },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){ Text(text = text, color = Color.White, fontSize = 25.sp)}
}

@Composable
fun LoadingScreen(isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}


