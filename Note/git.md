## 常用命令

git init  
将一个普通文件夹变成git仓库，此时文件夹下多出.git文件夹，表示可以使用git管理，此时这个文件夹称为git工作区  
或者  
使用git clone url(github上的仓库链接)将仓库从github上下载下来

### 当对工作区内的文件做出修改后
git add 文件名  
表示将该文件的修改加入到暂存区  
git add .  
(注意后面有个 . )表示将当前目录下的所有文件的修改都加入到暂存区

git commit -m "备注信息"  
表示将暂存区的修改提交到当前分支，提交之后暂存区清空

git push -u origin master  
将本仓库更新到github上

### 撤回修改
git reset -- 文件名  
使用当前分支上的修改覆盖暂存区，用来撤销最后一次 git add files  
git checkout -- 文件名  
使用暂存区的修改覆盖工作目录，用来撤销本地修改

### 删除
git rm 文件名  
删除暂存区和分支上的文件，同时工作区也不需要这个文件，之后commit保存到分支

如果不小心提交了不想提交的文件到github上, 此时想删除刚刚不小心提交的文件同时保留工作目录的文件  
git rm -r --cached 文件夹名  
删除暂存区的修改，之后再commit保存到分支