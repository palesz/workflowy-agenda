(ns agenda
  (:require [clj-http.client :as client]
            [clojure.pprint :as pp]
            [slingshot.slingshot :as slingshot]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(defn login
  "Logs in the user and returns with the cookie-store
  to be used in the subsequent requests."
  [username password]
  (let [cs (clj-http.cookies/cookie-store)
        response (client/post "https://workflowy.com/accounts/login/"
                              {:form-params  {:username username
                                              :password password}
                               :cookie-store cs})]
    cs))

(defn notes
  "Returns with all the notes stored in workflowy.
  This can be a big list, but we can use this later
  for filtering."
  [cookie-store & {:keys [client-version] :or {client-version 15}}]
  (let [response (client/get "https://workflowy.com/get_initialization_data"
                             {:cookie-store cookie-store
                              :query-params {:client_version client-version}})]
    (json/read-str (response :body))))

(defn workflowy-node->node
  "Converts the workflowy node, into an easier to remember,
  internal representation.

  Workflowy node information:
  \"no\": notes
  \"cp\": completed
  \"lm\": ???
  \"id\": uuid of the node
  \"nm\": content of the node
  \"ch\": children nodes
  "
  [wf-node]
  {:notes (wf-node "no")
   :completed? (not (nil? (wf-node "cp")))
   :lm (wf-node "lm")
   :id (wf-node "id")
   :content (wf-node "nm")})

(defn flatten-nodes
  [node & {:keys [max-level level] :or {max-level (Integer/MAX_VALUE) level 0}}]
  (if (< level max-level)
    (let [children (->> (node "ch")
                        (map #(flatten-nodes % :level (+ level 1) :max-level max-level))
                        (into []))]
      (->> [(dissoc node "ch") children]
           (flatten)))
    []))

(defn construct-root-node [workflowy-notes]
  {"ch" (((workflowy-notes "projectTreeData") "mainProjectTreeInfo") "rootProjectChildren")})

(defn flatten-notes [workflowy-notes]
  (let [root-node (construct-root-node workflowy-notes)]
    (->> (flatten-nodes root-node)
         (map workflowy-node->node)
         (into []))))

(defn info-pattern [prefix]
  (re-pattern (str "#" prefix "-([^\\s<]+)")))

(defn extract-info
  [node prefix]
  (->> (node :content)
       (str)
       (re-seq (info-pattern prefix))
       (map #(nth % 1))))

(defn has-pattern?
  [node pattern]
  (->> (node :content)
       (str)
       (re-seq (re-pattern pattern))
       (nil?)
       (not)))

(defn extract-agenda-information
  "Extracts the agenda information from the given node.
  #s-yyyy[-mm[-dd]]: Scheduled for the given day  --> :scheduled
  #d-yyyy[-mm[-dd]]: Deadline on the given day    --> :deadline
  #t-status: Status of the given task             --> :status
  #r-repeatintervaldefinition                     --> :repeat-interval
  #agenda-hide"
  [node]
  {:scheduled (first (extract-info node "s"))
   :deadline (first (extract-info node "d"))
   :status (first (extract-info node "t"))
   :repeat-interval (first (extract-info node "r"))
   :hide? (has-pattern? node "#agenda-hide")
   })

(defn enrich-nodes-with-agenda-info [nodes]
  (->> nodes
       (map #(assoc % :agenda-info (extract-agenda-information %)))))

