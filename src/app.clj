(ns app
  (:require agenda)
  (:import (java.nio.file Paths))
  (:gen-class))


;LONG TERM GOAL
;Show everything that had a deadline in the last 3 days and is DONE
;plus show everything that has a deadline in the next 7 days and is NOT DONE yet
;
;BUT
;for now, just list everything with a deadline, order by the deadline

(defn link-to-task [n]
  (str "https://workflowy.com/#/" (n :id)))

(defn print-group [[group-key todos]]
  (println "\n" group-key "---------------------------")
  (doseq [t todos]
    (println (str "TODO " (-> t :agenda-info :deadline) ": " (t :content) " " (link-to-task t)))))

(defn -main [& args]
  (time (let [settings (read-string (slurp (.toString (Paths/get (System/getProperty "user.home") (into-array ["workflowy-agenda.config"])))))
              session (agenda/login (settings :username) (settings :password))
              notes (agenda/notes session)
              flatten-nodes (agenda/flatten-notes notes)
              nodes-with-agenda (agenda/enrich-nodes-with-agenda-info flatten-nodes)]
          (->> nodes-with-agenda
               (filter #(not (% :completed?)))
               (filter #(not (-> % :agenda-info :hide?)))
               (filter #(-> % :agenda-info :deadline))
               (group-by #(->> [(-> % :agenda-info :deadline) (-> % :agenda-info :scheduled)]
                               (filter identity)
                               (sort-by identity compare)
                               (first)))
               (vec)
               (sort-by #(% 0))
               (map print-group)
               (doall))
          )))
