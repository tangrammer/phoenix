(ns leiningen.phoenix
  (:require [leiningen.uberjar :as u]
            [leinjacker.eval :refer [eval-in-project]]
            [clojure.java.io :as io]
            [phoenix.plugin :refer [select-project-keys]])
  (:import [java.io File]))

(defn server
  "Starts the Phoenix application, as per the configuration file specified in project.clj.

   Usage: lein phoenix [server]"
  
  [project]

  (let [project-subset (select-project-keys project)]
    (eval-in-project project
                     `(do
                        (#'phoenix/init-nrepl! (quote ~project-subset))
                        (phoenix/start!))
                     `(require '~'phoenix))))

(defn include-aotd-main [project]
  (update-in project [:filespecs] conj {:type :bytes
                                        :path "phoenix/main.class"
                                        :bytes (eval-in-project (assoc project :eval-in :classloader)

                                                                `(let [class-file# (clojure.java.io/file "/home/james/src/james/libraries/phoenix/runtime/target/classes/phoenix/main.class")
                                                                       file-size# (.length class-file#)
                                                                       buffer# (byte-array file-size#)]
                                                                   (with-open [is# (clojure.java.io/input-stream class-file#)
                                                                               dis# (java.io.DataInputStream. is#)]
                                                                     (.readFully dis# buffer#)
                                                                     buffer#))
                                                                
                                                                '(require 'clojure.java.io))}))

(defn uberjar-project-map [project]
  (-> project
      (update-in [:filespecs] conj {:type :bytes
                                    :path "META-INF/phoenix-config-resource"
                                    :bytes (:phoenix/config project)})
      (update-in [:filespecs] conj {:type :bytes
                                    :path "META-INF/phoenix-repl-options.edn"
                                    :bytes (pr-str (:repl-options project))})))

(defn build-system [project]
  (let [project-file (doto (File/createTempFile "phoenix-project" ".edn")
                       (.deleteOnExit))]
    (eval-in-project project

                     `(#'phoenix.build/build-system-main '~project ~(.getAbsolutePath project-file))

                     '(require 'phoenix.build))

    (with-meta (read-string (slurp project-file))
      (meta project))))

(defn uberjar
  "Creates an uberjar of the Phoenix application

   Usage: lein phoenix uberjar"
  [project]

  (let [built-project (-> project
                          build-system
                          include-aotd-main
                          uberjar-project-map)]
    (u/uberjar (-> built-project
                   (vary-meta #(assoc-in % [:without-profiles] built-project)))
               
               'phoenix.main)))

(defn phoenix
  "Plugin to configure and co-ordinate a Component-based system

  Usage: lein phoenix [server, uberjar]

  If no arguments are provided, 'server' is assumed.

  For more details of how to set up and use Phoenix, please refer to
  the documentation at https://github.com/james-henderson/phoenix"
  
  [project & [command & args]]

  (case command
    "server" (server project)
    "uberjar" (uberjar project)
    nil (server project)))
