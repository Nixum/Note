![git流程](https://github.com/Nixum/Java-Note/raw/master/picture/git_flow.jpg)

## 常用命令

* git init

  将一个普通文件夹变成git仓库，此时文件夹下多出.git文件夹，表示可以使用git管理，此时这个文件夹称为git工作区
  
  或者
  
  使用git clone url(github上的仓库链接)将仓库从github上下载下来

### 当对工作区内的文件做出修改后
* git add 文件名

  表示将该文件的修改加入到暂存区

* git add . 

  (注意后面有个 . )表示将当前目录下的所有文件的修改都加入到暂存区

* git commit -m "备注信息"

  表示将暂存区的修改提交到当前分支，提交之后暂存区清空

* git push -u origin master

  将分支上的修改更新到github上

### 撤回修改
* git log 查看提交记录，获取commit id

* git reset -- 文件名 或者 commitId

  使用当前分支上的修改覆盖暂存区，用来撤销最后一次 git add files

* git checkout -- 文件名

  使用暂存区的修改覆盖工作目录，用来撤销本地修改

### 删除
* git rm 文件名

  删除暂存区和分支上的文件，同时工作区也不需要这个文件，之后commit保存到分支

* git rm -r --cached 文件夹名

  删除暂存区的修改，之后再commit保存到分支，如果不小心提交了不想提交的文件到分支上，此时想删除刚刚不小心提交的文件同时保留工作目录的文件时使用

### 更新

* git pull origin master

  更新线上修改到本地分支

### 查看状态
* git status

  可以查看本地和分支哪些文件有修改

### 几个容易混淆的命令

* git rebase 和 git merge

效果都是合并，merge时可以看到被merge的分支以及其提交记录，不破坏被merge分支的代码提交记录；rebase是直接把两个分支合并，被合并的分支就像在原来的分支上开发一样，不会新增提交记录到目标分支，形成线性提交的历史记录。

合并代码到公共分支的时候使用git merge

合并代码到个人/功能分支时使用git rebase

* git pull 和 git fetch

效果都是更新远程仓库到本地，fetch是将远程仓库的内容拉到本地，用户检查只会再决定是否合并；pull是将远程仓库的内容拉到本地后直接合并，pull = fetch + merge

* git reset 和 git revert

效果都是撤销，reset回退版本会丢失回退前的提交信息，revert则不会，revert会重新创建一个新的提交，新的提交内容是指定的commitId对应的内容，原有的提交得到保留。

### 其他常用命令  

![git命令](https://github.com/Nixum/Java-Note/raw/master/picture/git_command.png)

## 参考

[廖雪峰 git教程](https://www.liaoxuefeng.com/wiki/0013739516305929606dd18361248578c67b8067c8c017b000 "")
