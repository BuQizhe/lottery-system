$cp = (Get-Content "D:\code\Project\lottery-system\classpath.txt" -Raw).Trim()
$argFile = "D:\code\Project\lottery-system\java-args.txt"
@"
-XX:TieredStopAtLevel=1
-Dspring.output.ansi.enabled=always
-Dfile.encoding=UTF-8
-classpath
$cp
com.example.lotterysystem.LotterySystemApplication
"@ | Out-File -FilePath $argFile -Encoding ascii
& "D:\Java\JDK\bin\java.exe" "@$argFile"
