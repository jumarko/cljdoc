(ns cljdoc.util.codox
  (:require [clojure.tools.logging :as log]))

(defn- index-by [k xs]
  (->> (for [[k vs] (group-by k xs)]
         (if (second vs)
           (throw (ex-info (format "Duplicate item for key %s: (files: %s)" k (mapv :file vs)) {:vs vs}))
           [k (first vs)]))
       (into {})))

(defn- macro? [var]
  (= :macro (:type var)))

(defn assert-no-duplicate-publics [namespaces]
  (doseq [ns namespaces]
    (index-by :name (:publics ns))))

(defn sanitize-macros
  "Reading macros from Clojure's and ClojureScripts `ns-publics` may
  return different arglists under certain conditions. Because macros
  are always coming from Clojure anyways we drop any macros that were
  retrieved from ClojureScript's ns-publics.

  Codox currently also makes two passes (clj/cljs) to include macros,
  presumably this was necessary in earlier versions whent he cljs analyzer
  didn't properly return macros as part of `ns-publics`.

  This function is idempotent and returns the analysis data in its
  original shape.

  See https://dev.clojure.org/jira/browse/CLJS-2852

  nomad 0.9.0-alpha9 is a good example that exhibits this issue:
  https://2950-119377591-gh.circle-artifacts.com/0/cljdoc-edn/jarohen/nomad/0.9.0-alpha9/cljdoc.edn"
  [{:strs [clj cljs] :as codox}]
  (assert-no-duplicate-publics clj)
  (assert-no-duplicate-publics cljs)
  (if (and clj cljs)
    (let [clj-by-name  (index-by :name clj)
          cljs-by-name (index-by :name cljs)]
      {"clj" clj
       "cljs" (doall
               (for [[name {:keys [publics] :as ns-data}] cljs-by-name
                     :let [cljs-publics-without-macros (remove macro? publics)
                           clj-macros (->> (get-in clj-by-name [name :publics])
                                           (filter macro?))]]
                 (do
                   ;; This has previously been an assertion but there are situations
                   ;; where an assert is not appropriate I believe.
                   ;; For instance fulcro.client.localized-dom defines macros
                   ;; in clojure but functions in cljs. Macros are
                   ;; also not loaded into the cljs namespace.
                   ;; see https://3616-119377591-gh.circle-artifacts.com/0/cljdoc-edn/fulcrologic/fulcro/2.6.0-RC8/cljdoc.edn
                   (when-not (= (set (map :name (filter macro? publics)))
                                (set (map :name clj-macros)))
                     (log/warnf "Unexpected macros between clj and cljs in %s" name))
                   (assoc ns-data :publics (into cljs-publics-without-macros clj-macros)))))})
    codox))