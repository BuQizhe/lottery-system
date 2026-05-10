@echo off
for /f "usebackq delims=" %%x in ("D:\code\Project\lottery-system\classpath.txt") do set "CP=%%x"
"D:\Java\JDK\bin\java.exe" -XX:TieredStopAtLevel=1 -Dspring.output.ansi.enabled=always -Dfile.encoding=UTF-8 -classpath "%CP%" com.example.lotterysystem.LotterySystemApplication
