package com.example.pre_capstone

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.example.pre_capstone.model.HistoryData
import com.example.pre_capstone.model.TimerSetting
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

//
class MyFireBase(){

    companion object{
        val dataBase = Firebase.database
        fun getTodayRef(uid: String) : DatabaseReference{

            val currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val formattedDate = currentTime.format(formatter)

            //todo 이후 유저 로그인시 유저이름으로 패스정하기
            val historyRef = dataBase.getReference("users")
                .child(uid)
                .child("history")
                .child(formattedDate)


            return historyRef
        }
    }
}

fun signInWithEmailAndPassword(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()

    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 로그인 성공
                onSuccess()
            } else {
                // 로그인 실패
                Log.e("firebaseError", "signInWithEmailAndPassword: ${task.exception}", )
                when (task.exception){
                    is FirebaseAuthInvalidUserException -> onError("존재하지 않는 계정입니다.")
                    is FirebaseAuthInvalidCredentialsException -> onError("이메일 또는 비밀번호가 올바르지 않습니다.")
                    else -> onError("기타오류입니다")
                }
            }
        }
}

fun signUp(context : Context, name : String, email: String, password: String,onSuccessCallback: () -> Unit ,onFailCallback : (Task<AuthResult>) -> Unit){
    var auth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i("firebase", "signUp: success")
                    val user = auth.currentUser
                user?.let { saveDefaultUserSettingFireBase(it.uid, name, context = context) }
                Log.d("login", "signUp: ${user?.uid}")
                onSuccessCallback()
            } else {
                Log.e("firebaseError", "signUp: ${task.exception}", )
                onFailCallback(task)
            }
        }
}
fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
    val auth = FirebaseAuth.getInstance()

    auth.fetchSignInMethodsForEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods ?: emptyList()
                callback(signInMethods.isNotEmpty()) // true면 이미 존재하는 이메일
            } else {
                callback(false) // 오류 발생 시 false 반환
            }
        }
}
fun loadWeekHistoryData(
    uid: String,
    today: LocalDateTime,
    callback: (List<List<Pair<Int, Int>>>) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val date = today.format(formatter)
    val weekDates = calcWeekDate(date)

    // 요일별 데이터를 저장할 맵 (인덱스를 키로 사용)
    val weekDataMap = mutableMapOf<Int, List<Pair<Int, Int>>>()
    var completedRequests = 0

    for (i in weekDates.indices) {
        val weekDate = weekDates[i]
        val dateHistoryRef = Firebase.database.getReference("users")
            .child(uid)
            .child("history")
            .child(weekDate)
            .child("totalData")

        loadDateHistoryData(dateHistoryRef) { data ->
            weekDataMap[i] = data
            completedRequests++

            if (completedRequests == weekDates.size) {
                // 모든 요청이 완료되면 인덱스 순으로 정렬된 리스트 생성
                val sortedWeekData = (0 until weekDates.size).map {
                    weekDataMap[it] ?: listOf(Pair(0, 0))
                }
                callback(sortedWeekData)
            }
        }
    }
}
fun loadDateHistoryData(historyRef: DatabaseReference, callback: (List<Pair<Int, Int>>) -> Unit) {
    var saveData = mutableListOf<Pair<Int, Int>>()
    historyRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val snapshot = task.result
            if (snapshot.exists()) {
                val data = snapshot.value
                if (data is Map<*, *>) {
                    val entries = data.entries
                    for (entry in entries) {
                        val value = entry.value as Map<*, *>
                        val workingMinute = value.get("first") as? Long ?: 1
                        val restMinute = value.get("second") as? Long ?: 1
                        saveData.add(Pair(workingMinute.toInt(), restMinute.toInt()))
                    }
                } else if (data is List<*>) {
                    for (item in data) {
                        if (item is Map<*, *>) {
                            val workingMinute = item.get("first") as? Long ?: 1
                            val restMinute = item.get("second") as? Long ?: 1
                            saveData.add(Pair(workingMinute.toInt(), restMinute.toInt()))
                        }
                    }
                }
                callback(saveData)
            } else {
                saveData.add(Pair(1, 1))
                println("No data available")
                callback(saveData)
            }
        } else {
            saveData.add(Pair(1, 1))
            Log.e("firebaseError", "loadDateHistoryData: ${task.exception?.message}", )
            callback(saveData)
        }
    }
}

fun calcWeekDate(inputDate: String): List<String> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val date = LocalDate.parse(inputDate, formatter)

    val startOfWeek = date.minusDays(date.dayOfWeek.value.toLong() - 1)

    val weekDates = mutableListOf<String>()
    for (i in 0 until 7) {
        weekDates.add((startOfWeek.plusDays(i.toLong())).format(formatter))
    }
    return weekDates
}


fun saveHistoryData(saveData : HistoryData, uid : String){
    val historyRef = MyFireBase.getTodayRef(uid = uid).push()
    historyRef.setValue(saveData)
        .addOnSuccessListener { Log.i("firebase", "saveHistoryData: save Success")}
        .addOnFailureListener {e ->  Log.e("firebaseError", "saveHistoryData: ${e.message}", ) }
    updateTodayHistoryData(saveData, uid)
}
fun updateTodayHistoryData(data: HistoryData, uid : String){
    val historyRef = MyFireBase.getTodayRef(uid = uid)
        .child("totalData")
        .child("${data.category}")

    var loadData : Pair<Int, Int> = Pair(0,0)
    loadTodayHistoryData(historyRef){x -> loadData = x
        val saveData = Pair(loadData.first + data.workingMinute, loadData.second + data.restMinute)
        historyRef.setValue(saveData)
            .addOnSuccessListener { Log.i("firebase", "saveHistoryData: save Success")}
            .addOnFailureListener {e ->  Log.e("firebaseError", "saveHistoryData: ${e.message}", ) }
    }



}


fun saveDefaultUserSettingFireBase(uid: String, name : String, context: Context) {

    val userRef  =MyFireBase.dataBase.getReference("users").child(uid)
    val timerSettingsRef = userRef
        .child("settings")
        .child("timer")

    val timerSettings = listOf(
        TimerSetting(0, "공부", context.getColor(R.color.myCategory1).toLong(), 50 * 60, 10 * 60),
        TimerSetting(1, "운동", context.getColor(R.color.myCategory2).toLong(), 3 * 60, 1 * 60),
        TimerSetting(2, "독서", context.getColor(R.color.myCategory3).toLong(), 55 * 60, 5 * 60),
        TimerSetting(3, "취미", context.getColor(R.color.myCategory4).toLong(), 30 * 60, 10 * 60),
        TimerSetting(4, "영단어", context.getColor(R.color.myCategory5).toLong(),  15* 60, 5 * 60)
    )

    val updates = HashMap<String, Any>()
    timerSettings.forEach { setting ->
        updates["${setting.index}"] = setting.toMap()
    }

    userRef.child("name").setValue(name)
    timerSettingsRef.updateChildren(updates)
        .addOnSuccessListener {
            Log.i("firebase", "saveDefaultUserSettingFireBase: saveName")

        }
        .addOnFailureListener { e ->
            Log.e("firebaseError", "saveDefaultUserSettingFireBase: ${e.message}", )
        }
}

fun loadTimerSettingsFireBase(uid: String, onSuccess: (List<TimerSetting>) -> Unit, onFailure: (Exception) -> Unit) {
    Firebase.database.reference
        .child("users")
        .child(uid)
        .child("settings")
        .child("timer")
        .get()
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val result = task.result
                Log.i("firebase", "loadTimerSettingsFireBase: ${result}")
                if (result.exists()) {
                    val timerSettings = mutableListOf<TimerSetting>()
                    for (snapshot in result.children) {
                        val setting = snapshot.getValue(TimerSetting::class.java)
                        setting?.let { timerSettings.add(it) }
                    }
                    onSuccess(timerSettings)
                } else {
                    onSuccess(emptyList()) // 데이터가 없을 경우 빈 리스트 반환
                }
            } else {
                onFailure(task.exception ?: Exception("알 수 없는 오류"))
            }
        }
}

fun loadUserName(uid: String, onComplete: (String?) -> Unit) {
    val userRef = MyFireBase.dataBase.getReference("users").child(uid)

    userRef.child("name").get()
        .addOnSuccessListener { dataSnapshot ->
            val name = dataSnapshot.value as? String
            onComplete(name)
            //Log.i("firebase", "loadUserName: 성공적으로 이름을 로드했습니다. ${name}")
        }
        .addOnFailureListener { e ->
            Log.e("firebaseError", "loadUserName: ${e.message}")
            onComplete(null)
        }
}

fun loadTodayHistoryData(historyRef : DatabaseReference, callback : (Pair<Int, Int>) -> Unit){
    var saveData = Pair(0,0)
    historyRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val snapshot = task.result
            if (snapshot.exists()) {
                val data = snapshot.value as Map<*, *>
                val workingMinute = data.get("first") as? Long ?: 0
                val restMinute = data.get("second") as? Long ?: 0
                Log.i("firebase", "loadTodayHistoryData: data - $data work - $workingMinute, rest - $restMinute")
                saveData = Pair(workingMinute.toInt(), restMinute.toInt())
                callback(saveData)
            } else {
                println("No data available")
                callback(saveData)
            }
        } else {
            println("Error getting data: ${task.exception}")
        }
    }
}



fun TimerSetting.toMap(): Map<String, Any> {
    return mapOf(
        "index" to index,
        "name" to name,
        "backgroundColor" to backgroundColor,
        "workTime" to workTime,
        "restTime" to restTime
    )
}

fun logOut(context : Context){

    // 1. Firebase 로그아웃
    FirebaseAuth.getInstance().signOut()

    // 2. 로컬 데이터 초기화
    clearLocalData(context)

    // 로그 출력
    println("로그아웃 완료")
}
private fun clearLocalData(context: Context) {
    // SharedPreferences 초기화
    val sharedPreferences = context.getSharedPreferences("APP_PREFERENCES", Context.MODE_PRIVATE)
    sharedPreferences.edit().clear().apply()

}
