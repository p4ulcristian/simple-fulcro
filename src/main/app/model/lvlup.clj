(ns app.model.lvlup
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    ;[taoensso.timbre :as log]
    [clojure.spec.alpha :as s]))

(def games {1 {:game/id 1 :game/name "Overwatch"}
            2 {:game/id 2 :game/name "Fallout 2"}
            3 {:game/id 3 :game/name "Fallout 3"}})

(def players {1 {:player/id 1 :player/name "Bela2" :player/game {:game/id 1}}
              2 {:player/id 2 :player/name "Pistidsa" :player/game 2}
              3 {:player/id 3 :player/name "Magus" :player/game 3}})

(def game-stations {1 {:game-station/id 1 :game-station/name "Crusader" :game-station/game {:game/id 1} :game-station/player {:player/id 1}}
                    2 {:game-station/id 2 :game-station/name "Zelda" :game-station/game {:game/id 2} :game-station/player {:player/id 2}}
                    3 {:game-station/id 3 :game-station/name "Magus" :game-station/game {:game/id 3} :game-station/player {:player/id 3}}
                    4 {:game-station/id 3 :game-station/name "Rubick" :game-station/game {:game/id 2}}})


(defresolver player-resolver [{:keys [db]} {:player/keys [id]}]
             {::pc/input  #{:player/id}
              ::pc/output [:player/id :player/name :player/game]}
             (get players id))

(defresolver game-station-resolver [{:keys [db]} {:game-station/keys [id]}]
             {::pc/input  #{:game-station/id}
              ::pc/output [:game-station/id :game-station/name :game-station/player]}
             (get game-stations id))

(defresolver game-resolver [{:keys [db]} {:game/keys [id]}]
             {::pc/input  #{:game/id}
              ::pc/output [:game/id :game/name]}
             (get games id))






(defresolver time-resolver [{:keys [db]} {:server/keys [time]}]
             {::pc/output [:server/time]}
             {:server/time (java.util.Date.)})

(defresolver all-players-resolver [{:keys [db]} input]
             {::pc/output [{:all-players [:player/id]}]}
             {:all-players (mapv second players)})

(defresolver all-games-resolver [{:keys [db]} input]
             {::pc/output [{:all-games [:game/id :game/name]}]}
             {:all-games (mapv second games)})

(defresolver all-game-stations-resolver [{:keys [db]} input]
             {::pc/output [{:all-game-stations [:game-station/id :game-station/name]}]}
             {:all-game-stations (mapv second game-stations)})

(defmutation make-older [env {::keys [id]}]
  {::pc/params [:person/id]
   ::pc/output [:person/id :person/age]})


(def resolvers [
                time-resolver
                all-players-resolver all-games-resolver all-game-stations-resolver
                player-resolver game-station-resolver game-resolver])