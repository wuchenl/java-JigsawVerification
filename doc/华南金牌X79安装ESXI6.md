#使用华南金牌X79烈焰战神安装ESXI6.7

##前置介绍
### vSphere Hypervisor
```
vSphere Hypervisor是Vmware公司推出的免费版ESXI。最新版本为6.7。
去官网注册后下载即可，下载时页面有免费序列号。
```
### X79平台
```
华南金牌X79主板，是基于X79平台的寨板。烈焰战神等同于他的尊贵版。单U最高版本。
在搭配2011CPU时，性价比还行。这里关于为啥不用amd等最新CPU，因为穷。洋垃圾便宜。
```
##配置清单

```
CPU：E5-2690V2  1300
内存：三星DDR3 1866 16G*2   240*2 
主板：华南金牌X79烈焰战神  680
电源：先马金牌550W  330
硬盘：Acer 512G SSD(后续更换为机械，组Raid5) 380
加上机箱等目前成本3500左右
```
##安装准备
```
1.首先去Vmware官网注册个人账号，并下载vSphere Hypervisor6.7的ISO包。
2.华南X79网卡型号为RealTek 8011系列，默认不支持，需要下载替换网卡驱动（下载地址:http://vibsdepot.v-front.de/depot/RTL/net55-r8168/net55-r8168-8.045a-napi.x86_64.vib）
3.下载ESXi-Customizer-v2.7.2 该软件为定制个性化ESXI所需要的。
4.ESXi-Customizer-v2.7.2 会有提示不支持WIN10等系统。打开其cmd文件。删除中间系统判断即可。
5.改完后启动他，选择对应的ISO包和对应的网卡驱动，然后选择生成文件放置位置。最后取消最下方版本检测。点击Run 即可。(虽然会提示不支持6.x，但实际支持)。
6.下载个大白菜等PE工具。制作一个启动盘。把制作好的ISO文件丢进对应的ISO目录。
```
###ESXi-Customizer-v2.7.2删除系统校验(win7以上系统需要此操作)
    删除ESXi-Customizer.cmd文件中200多行的下述内容即可。
```
   if /I not "%1"=="silent" call :logCons Checking Windows version ...
   for /F "tokens=3,4,5 delims=. " %%a in ('ver') do (
      set WinVer=%%b.%%c
      if "%%a"=="2000" set WinVer=5.0
      if "%%a"=="XP" set WinVer=5.1
   )
   if /I "%1"=="silent" goto :eof
   if "!WinVer!"=="5.0" call :logCons --- INFO: Running on Windows 2000. What?!
   if "!WinVer!"=="5.1" call :logCons --- INFO: Running on Windows XP.
   if "!WinVer!"=="5.2" call :logCons --- INFO: Running on Windows Server 2003.
   if "!WinVer!"=="6.0" call :logCons --- INFO: Running on Windows Vista or Server 2008.
   if "!WinVer!"=="6.1" call :logCons --- INFO: Running on Windows 7 or Server 2008 R2.
   if "!WinVer!"=="6.2" call :logCons --- INFO: Running on Windows 8 or Server 2012.
   if "!WinVer!"=="6.3" call :logCons --- INFO: Running on Windows 8.1 or Server 2012 R2.
   if "!WinVer!" GTR "6.3" call :logCons --- WARNING: Running on a Windows newer than 8.1 / 2012 R2. Don't know if this will work ...
   if "!WinVer!" LSS "5.1" call :earlyFatal Unsupported Windows Version: !WinVer!. At least Windows XP is required & exit /b 1
   if "!WinVer!" NEQ "6.1" call :logCons --- WARNING: Your Windows version is supported for customizing ESXi 5.x, but not ESXi 4.1.
```

##安装
    这个时候，我们应该已经有了一个配置无问题的2011平台PC一台。以及一个做好了引导，且有我们改后ISO包的启动盘。
```
1.开机点击DEL键，进入BIOS。
2.右移进入BOOT选项，下方有对应的启动顺序选择。
3.F10保存重启加载到启动盘选择界面。(这里我是用的大白菜。)
4.选择安装其他ISO(非WIN的安装，类似的功能即可。)
5.选择后提示找不到文件，因为引导的路径和我们放置的路径可能不对应。按照提示输入'h' 回车查看帮助即可
6.输入"/ISO" 到对应ISO目录。(此处是因为我iso包放置在了U盘ISO目录)。
7.如果上述没问题后，会扫描出对应ISO包，选择进行安装即可。
8.一直自动化即可。到后续选择几个对应的选项即可(比如安装硬盘，键盘布局，网络获取，用户名密码输入等)
9.一切完成后重启，等待系统加载完成后，F2，输入用户名密码，默认root。能看到本机对应的ip地址，同网段直接访问管理即可。
10.通过浏览器访问其ip，输入用户名密码后，左侧会菜单栏，找到设置或者管理，证书。添加我们下载时的证书即可。
PS: 以前的6.0的企业版证书什么的也可以用。比免费的支持的功能更多。有兴趣直接百度ESXI6.7 key 即可。
```