@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM InvestLab 构建和运行脚本 (Windows)
REM 用途: 打包前端和后端项目,并启动服务供本地测试

echo ======================================
echo    InvestLab 构建和运行脚本
echo ======================================
echo.

REM 设置项目路径
set "PROJECT_ROOT=%~dp0"
set "BACKEND_DIR=%PROJECT_ROOT%backend"
set "FRONTEND_DIR=%PROJECT_ROOT%frontend"
set "DIST_DIR=%PROJECT_ROOT%dist"

REM 1. 检查 Java
echo [INFO] 检查 Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java 未安装,请先安装 JDK 21+
    pause
    exit /b 1
)
echo [SUCCESS] Java 检查通过

REM 2. 检查 Node.js
echo [INFO] 检查 Node.js...
node -v >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js 未安装,请先安装 Node.js 18+
    pause
    exit /b 1
)
echo [SUCCESS] Node.js 检查通过

REM 3. 创建输出目录
echo [INFO] 创建输出目录...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"

REM 4. 构建后端
echo [INFO] 开始构建后端项目...
cd /d "%BACKEND_DIR%"

if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat 文件不存在
    pause
    exit /b 1
)

call gradlew.bat clean build -x test
if errorlevel 1 (
    echo [ERROR] 后端构建失败
    pause
    exit /b 1
)

echo [SUCCESS] 后端构建成功
if exist "build\libs\investlab-backend-0.0.1.jar" (
    copy "build\libs\investlab-backend-0.0.1.jar" "%DIST_DIR%\investlab-backend.jar" >nul
) else (
    copy "build\libs\*-0.0.1.jar" "%DIST_DIR%\investlab-backend.jar" >nul
)
echo [SUCCESS] 后端 jar 文件已复制到 dist 目录

REM 5. 构建前端
echo [INFO] 开始构建前端项目...
cd /d "%FRONTEND_DIR%"

REM 安装依赖(如果需要)
if not exist "node_modules" (
    echo [INFO] 安装前端依赖...
    call npm install
)

call npm run build
if errorlevel 1 (
    echo [ERROR] 前端构建失败
    pause
    exit /b 1
)

echo [SUCCESS] 前端构建成功
xcopy /E /I /Y "dist" "%DIST_DIR%\frontend" >nul
echo [SUCCESS] 前端构建文件已复制到 dist 目录

REM 6. 创建启动脚本
echo [INFO] 创建启动脚本...

REM 创建后端启动脚本
echo @echo off > "%DIST_DIR%\start-backend.bat"
echo cd /d "%%~dp0" >> "%DIST_DIR%\start-backend.bat"
echo echo 正在启动后端服务... >> "%DIST_DIR%\start-backend.bat"
echo java -jar investlab-backend.jar >> "%DIST_DIR%\start-backend.bat"
echo pause >> "%DIST_DIR%\start-backend.bat"

REM 创建前端启动脚本
echo @echo off > "%DIST_DIR%\start-frontend.bat"
echo cd /d "%%~dp0\frontend" >> "%DIST_DIR%\start-frontend.bat"
echo echo 正在启动前端服务... >> "%DIST_DIR%\start-frontend.bat"
echo echo 访问地址: http://localhost:8081 >> "%DIST_DIR%\start-frontend.bat"
echo python -m http.server 8081 >> "%DIST_DIR%\start-frontend.bat"
echo pause >> "%DIST_DIR%\start-frontend.bat"

REM 创建一键启动脚本
echo @echo off > "%DIST_DIR%\start-all.bat"
echo chcp 65001 ^>nul >> "%DIST_DIR%\start-all.bat"
echo cd /d "%%~dp0" >> "%DIST_DIR%\start-all.bat"
echo echo ====================================== >> "%DIST_DIR%\start-all.bat"
echo echo    InvestLab 启动脚本 >> "%DIST_DIR%\start-all.bat"
echo echo ====================================== >> "%DIST_DIR%\start-all.bat"
echo echo. >> "%DIST_DIR%\start-all.bat"
echo echo 正在启动后端服务... >> "%DIST_DIR%\start-all.bat"
echo start "InvestLab Backend" /MIN cmd /c "java -jar investlab-backend.jar ^> backend.log 2^>^&1" >> "%DIST_DIR%\start-all.bat"
echo echo 后端服务已启动 >> "%DIST_DIR%\start-all.bat"
echo echo 后端地址: http://localhost:8080 >> "%DIST_DIR%\start-all.bat"
echo echo. >> "%DIST_DIR%\start-all.bat"
echo echo 等待后端服务启动... >> "%DIST_DIR%\start-all.bat"
echo timeout /t 8 /nobreak ^> nul >> "%DIST_DIR%\start-all.bat"
echo echo. >> "%DIST_DIR%\start-all.bat"
echo echo 正在启动前端服务... >> "%DIST_DIR%\start-all.bat"
echo start "InvestLab Frontend" /MIN cmd /c "cd frontend ^&^& python -m http.server 8081 ^> ../frontend.log 2^>^&1" >> "%DIST_DIR%\start-all.bat"
echo echo 前端服务已启动 >> "%DIST_DIR%\start-all.bat"
echo echo 前端地址: http://localhost:8081 >> "%DIST_DIR%\start-all.bat"
echo echo. >> "%DIST_DIR%\start-all.bat"
echo echo ====================================== >> "%DIST_DIR%\start-all.bat"
echo echo    服务启动完成! >> "%DIST_DIR%\start-all.bat"
echo echo ====================================== >> "%DIST_DIR%\start-all.bat"
echo echo. >> "%DIST_DIR%\start-all.bat"
echo echo 后端 API: http://localhost:8080 >> "%DIST_DIR%\start-all.bat"
echo echo 前端应用: http://localhost:8081 >> "%DIST_DIR%\start-all.bat"
echo echo. >> "%DIST_DIR%\start-all.bat"
echo echo 正在打开浏览器... >> "%DIST_DIR%\start-all.bat"
echo timeout /t 2 /nobreak ^> nul >> "%DIST_DIR%\start-all.bat"
echo start http://localhost:8081 >> "%DIST_DIR%\start-all.bat"
echo echo. >> "%DIST_DIR%\start-all.bat"
echo echo 按任意键关闭此窗口(服务将继续在后台运行) >> "%DIST_DIR%\start-all.bat"
echo pause ^> nul >> "%DIST_DIR%\start-all.bat"

REM 创建 README
echo InvestLab 运行说明 > "%DIST_DIR%\README.txt"
echo ================== >> "%DIST_DIR%\README.txt"
echo. >> "%DIST_DIR%\README.txt"
echo 快速启动: >> "%DIST_DIR%\README.txt"
echo --------- >> "%DIST_DIR%\README.txt"
echo. >> "%DIST_DIR%\README.txt"
echo 方式一: 一键启动(推荐) >> "%DIST_DIR%\README.txt"
echo 双击运行 start-all.bat >> "%DIST_DIR%\README.txt"
echo. >> "%DIST_DIR%\README.txt"
echo 方式二: 分别启动 >> "%DIST_DIR%\README.txt"
echo 1. 双击 start-backend.bat 启动后端 >> "%DIST_DIR%\README.txt"
echo 2. 双击 start-frontend.bat 启动前端 >> "%DIST_DIR%\README.txt"
echo. >> "%DIST_DIR%\README.txt"
echo 访问地址: >> "%DIST_DIR%\README.txt"
echo --------- >> "%DIST_DIR%\README.txt"
echo 后端 API: http://localhost:8080 >> "%DIST_DIR%\README.txt"
echo 前端应用: http://localhost:8081 >> "%DIST_DIR%\README.txt"
echo. >> "%DIST_DIR%\README.txt"
echo 注意事项: >> "%DIST_DIR%\README.txt"
echo --------- >> "%DIST_DIR%\README.txt"
echo 1. 需要安装 Java 21+ >> "%DIST_DIR%\README.txt"
echo 2. 需要安装 Python 3 >> "%DIST_DIR%\README.txt"
echo 3. 确保 8080 和 8081 端口未被占用 >> "%DIST_DIR%\README.txt"

echo [SUCCESS] 启动脚本创建完成

REM 7. 显示构建结果
echo.
echo ======================================
echo [SUCCESS] 构建完成!
echo ======================================
echo.
echo 构建产物位置: %DIST_DIR%
echo.
echo 文件列表:
echo   - investlab-backend.jar    (后端 jar 文件)
echo   - frontend\                (前端静态文件)
echo   - start-all.bat            (一键启动脚本)
echo   - start-backend.bat        (后端启动脚本)
echo   - start-frontend.bat       (前端启动脚本)
echo   - README.txt               (使用说明)
echo.

REM 8. 询问是否立即启动
set /p "REPLY=是否立即启动服务进行测试? (y/n): "
if /i "%REPLY%"=="y" (
    echo.
    echo [INFO] 正在启动服务...
    cd /d "%DIST_DIR%"
    call start-all.bat
) else (
    echo.
    echo [INFO] 稍后可以进入 dist 目录双击 start-all.bat 启动服务
    echo.
    pause
)
