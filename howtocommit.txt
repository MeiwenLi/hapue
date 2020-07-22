The current active and default branch is “develop”. Make sure all your feature/bug fix code commits being made to this branch and submitted via a pull request. 

At this moment, we need 1 reviewer to review your code. This number could be changed in future.

Following is a quick guide on doing your code commits.(some for first time use only)

1. Clone the repository
            git clone https://github.com/MeiwenLi/hapue.git
2. Create your own branch from “develop” and change your working branch to it.
            git branch reporter
            git branch
            git checkout reporter

  (Here you might make some code change) 

3. Add changes for commit,
            changes from particular file 
                git add <file>  (eg.  git add howtocommit.txt)
            changes from all tracked and untracked files
                git add -A 
            remove the added file to be changed
                git reset HEAD <file>… (eg.  git reset HEAD src/.DS_Store)
Please do not change existing dependencies version in POM.xml but only add new ones if needed
4. Make sure your code change will be on the latest code base
            git pull 
5. Confirm the changes you want to make
            git status
6. Commit and push your code to your own branch with descriptions better including purpose, risk of your set of code.
            git commit -m "test the repository branches setting”
            git push <--set-upstream origin reporter>
7. You can choose to delete your branch or keep it for next use. If you keep it, please make sure you pull latest code before use next time!

To remove the last commit from git, you can simply run git reset --hard HEAD^ If you are removing multiple commits from the top, you can run git reset --hard HEAD~2 to remove the last two commits.

Then you will need to make a pull request and require a code review from the GitHub website (you might want to ping the people to help you review code in order to merge it to develop branch)
When you get review approval, you code will be automatically merged to “develop” branch.

Some youtube you might want to watch regarding collaboration through GitHub.
https://www.youtube.com/watch?v=8fx-EaOUK2E
