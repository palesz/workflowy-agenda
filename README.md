# workflowy-agenda

## Workflowy tags

This agenda application uses simple tags to figure out the scheduled time and deadline about the tasks you
had created.

* Add a #d-yyyy-mm-dd tag to your task in workflowy to indicate the deadline on the task.
* Add a #s-yyyy-mm-dd tag to your task to indicate the next scheduled time for this task (when do you want to touch it next).

## How to run
A clojure client application that is capable querying workflowy.com and extracting some agenda from it.

to run it, just create a lein uberjar first:

    lein uberjar
    
After that, create a workflowy-agenda.config file in your home directory:

    cd ~
    vim workflowy-agenda.config
    
with this content:

    {
      :username "your-workflowy-account-email-addr"
      :password "your-workflowy-password"
    }
    
and now, start it:

    java -jar target/workflowy.....-standalone.jar
