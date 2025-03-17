package com.example.pre_capstone
import MainPage
import android.content.BroadcastReceiver
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth


val LocalNavController = compositionLocalOf<NavHostController> { error("No NavController found!") }


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview
@Composable
fun MyBottomNavi(navController: NavHostController = rememberNavController()) {

//todo 처음 시작점잡기
  /*
    * 1.구현 - 현 계정 있는지에따라 사용자 정보 로드하고 메인화면가기 -> 테스트
    * 2.구현 - 계정없을 시 로그인창가기
    * 3.구현 - 로그인창or 회원가입창에서 메인창 이동시 정보로드하고 메인화면가기
    * 4.구현 - 메인화면에서 버튼 동적으로 생성, 이름 + 시간도 적기\
    *
    *
    * 7.구현 -  로그아웃 구현(버튼 -> 로그아웃 -> 로그인창)
    *
    * 10.디자인 - 애니 로딩창
    *
    *
    *
    * - - - - - - - - - - - 해결된것들 - - - - - - - - - - - -
    *
    *
    *
    * 5.구현 -  일타이머 끝날시 진동 + 소리 이벤트
    * 6.구현 -  기록창 다시만들기 주간 기록 볼 수 있도록하기
    *
    * 8.디자인 - 타이머 원그리기 변경(3등분해서 하기 + 쉬는시간은 타이머표시(60분넘길시 아이콘으로 변경))
    * 9.디자인 - 종료버튼시 애니메이션 넣고 다시 돌아가기(불몇번 붙였는지 배열로 디자인하면 될듯)
    *
    * 11.구현 -  기록창 달별로 보기 커밋마냥 보기 -> 0, 1-3, 4-6, 7-9, 10+
    *
    *
    *
    * 1.디자인 - 워치 앱 기본디자인설정
    * 2.구현 - 타이머정보 연동(타이머 이름, 색상)
    * 3.구현 - 타이머생성
    * 4.구현 - 타이머
    *
    *
    *
    *
    *
    * 생각할 것
    * 메인화면 갈때마다 정보 업데이트할것
    * 타이머 정보 저장은 언제할것인가
    * */

    val user = FirebaseAuth.getInstance().currentUser
    val startPage = if(user != null) "main" else "signin"


    CompositionLocalProvider(LocalNavController provides navController) {
        val currentRoute = navController.currentBackStackEntryAsState()?.value?.destination?.route

        Scaffold(
            bottomBar ={
                if(currentRoute in listOf("main","history" ))
                {
                    BottomNavigation(
                        backgroundColor = colorResource(R.color.myGray)
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        BottomNavigationItem(
                            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                            label = { Text("Main") },
                            selected = currentDestination?.hierarchy?.any { it.route == "main" } == true,
                            onClick = {
                                navController.navigate("main") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            selectedContentColor = colorResource(R.color.myButtonColor),
                            unselectedContentColor = Color.Gray
                        )

                        BottomNavigationItem(
                            icon = { Icon(Icons.Filled.List, contentDescription = "History") },
                            label = { Text("History") },
                            selected = currentDestination?.hierarchy?.any { it.route == "history" } == true,
                            onClick = {
                                navController.navigate("history") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            selectedContentColor = colorResource(R.color.myButtonColor),
                            unselectedContentColor = Color.Gray
                        )
                    }


                }
                else null
            }

        ) { innerPadding ->

            NavHost(navController, startDestination = startPage, Modifier.padding(innerPadding)) {
                composable("main") { MainPage(navHostController = navController) }
                composable("history") {
                    weekHistoryApp() }
                composable("signup") { signupPage() }
                composable("signin") { LoginScreen() }


                //todo이후 사용자에 최대칸 넣어서 실행할것 추후 최적화

                    composable(
                        route = "timer/{timerName}/{workTime}/{restTime}/{category}",
                        arguments = listOf(
                            navArgument("timerName") { type = NavType.StringType },
                            navArgument("workTime") { type = NavType.IntType },
                            navArgument("restTime") { type = NavType.IntType },
                            navArgument("category") { type = NavType.IntType }
                        )
                    ) { entry ->
                        val timerName = entry.arguments?.getString("timerName") ?: ""
                        val workTime = entry.arguments?.getInt("workTime") ?: 60
                        val restTime = entry.arguments?.getInt("restTime") ?: 10
                        val category = entry.arguments?.getInt("category") ?: 0

                        DualStopwatchApp(
                            timerName = timerName,
                            workTime = workTime,
                            restTime = restTime,
                            category = category
                        )
                    }
            }
        }
    }

}


