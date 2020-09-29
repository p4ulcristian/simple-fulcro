(ns app.server-components.http-server
  (:require
    [app.server-components.config :refer [config]]
    [app.server-components.middleware :refer [middleware]]
    [mount.core :refer [defstate]]
    [clojure.pprint :refer [pprint]]
    [org.httpkit.server :as http-kit]
    [ring.middleware.reload :refer [wrap-reload]]
    [taoensso.timbre :as log]))

(defstate http-server
  :start
  (let [cfg (::http-kit/config config)]
    (log/info "Starting HTTP Server with config " (with-out-str (pprint cfg)))
    (http-kit/run-server (wrap-reload #'middleware)
                         cfg))
  :stop (http-server))
