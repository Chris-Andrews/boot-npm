(ns degree9.boot-npm
  (:require [boot.core :as boot]
            [boot.tmpdir :as tmpd]
            [boot.util :as util]
            [degree9.boot-exec :as exec]
            [clojure.java.io :as io]
            [boot.task.built-in :as tasks]
            [cheshire.core :refer :all]))

(boot/deftask npm
  "boot-clj wrapper for npm"
  [f npm         VAL     str      "A package.json file to parse."
   i install     FOO=BAR {kw str} "Dependency map."
   d development         bool     "Include development dependencies with packages."
   r dry-run             bool     "Report what changes npm would have made. (usefull with boot -vv)"
   g global              bool     "Opperates in global mode. Packages are installed to prefix."
   c cache-key   VAL     kw       "Optional cache key for when npm is used with multiple dependency sets."]
  (let [npmjsonf  (:npm         *opts* "./package.json")
        deps      (:install     *opts*)
        dev       (:development *opts*)
        global    (:global      *opts*)
        cache-key (:cache-key   *opts* ::cache)
        tmp       (boot/cache-dir! cache-key)
        tmp-path  (.getAbsolutePath tmp)
        npmjson   (generate-string {:name "boot-npm" :version "0.1.0" :dependencies deps})
        args      (cond-> ["install"]
                    (not dev) (conj "--production")
                    dry-run   (conj "--dry-run")
                    global    (conj "--global"))]
    (comp
      (exec/properties :contents npmjson :directory tmp-path :file npmjsonf)
      (exec/exec :process "npm" :arguments args :directory tmp-path :local "bin"))))

(boot/deftask exec
  "Exec wrapper for npm modules"
  [m module         VAL  str      "NPM node module."
   p process        VAL  str      "CLI executable of the npm module. (if different than module name)"
   d version        VAL  str      "Module version string."
   a arguments      VAL  [str]    "List of arguments to pass to cli process."
   g global              bool     "Opperates in global mode. Packages are installed to global location."
   c cache-key      VAL  kw       "Optional cache key for when npm is used with multiple dependency sets."]
  (let [module    (:module    *opts*)
        process   (:process   *opts* module)
        version   (:version   *opts* "*")
        args      (:arguments *opts*)
        global    (:global    *opts*)
        cache-key (:cache-key *opts* ::cache)
        install   (assoc {} (keyword module) version)
        tmp       (boot/cache-dir! cache-key)
        tmp-path  (.getAbsolutePath tmp)]
    (comp
      (npm :install install :cache-key cache-key :global global)
      (exec/exec :process process :arguments args :cache-key cache-key :local (str "node_modules/" module "/bin")))))
