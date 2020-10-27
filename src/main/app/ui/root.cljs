(ns app.ui.root
  (:require
    [app.model.session :as session]
    [clojure.string :as str]

    [re-frame.core :refer [reg-sub dispatch dispatch-sync subscribe reg-event-db reg-event-fx inject-cofx path after subscribe dispatch dispatch-sync reg-fx]]
    [reagent.core :as r]
    [ajax.core :refer [GET POST] :as ajax]
    [app.application :refer [web-app]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button b]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro-css.css :as css]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [taoensso.timbre :as log]))



(def default-db {:eql {}

                 :idents {:player/id {1 {:player/id 1 :player/name "Paul"}
                                      2 {:player/id 2 :player/name "Gabor"}}}})

(dispatch [:initialize-db])

(reg-event-fx
  :initialize-db
  ;[(inject-cofx :get-local-storage)]
  (fn [{:keys [db local-storage]} _]
    {:db default-db}))



(reg-event-db
  :add-eql-model
  (fn [db [_ {:keys [model query ident]}]]
    (assoc-in db [:eql model] {:ident ident
                               :query query})))

(reg-event-db
  :assoc
  (fn [db [_ the-key the-value]]
    (assoc db the-key the-value)))



;(ajax-post {:url           (str "/admin/" type "/modify")
;                    :body          form-data
;                    :handler       #(do (dispatch [:assoc-item type (read-string %)])
;                                        (dispatch [:call-snackbar {:message "Sikeres módosítás!"
;                                                                   :type    "success"}]))


(defn ajax-post [{:keys [url handler error-handler params body response-format]}]
  (POST url
        {:handler         handler
         :body            body
         :params          params
         :response-format response-format
         :error-handler   error-handler}))





(defn build-eql-query [model all-models]
  (let [this (:query (get all-models model))]
    (reduce
      (fn [till-now next]
        (vec (conj till-now (cond
                              (keyword? next) next
                              (map? next) (let [[to-join joined] (first next)]
                                            (assoc {} to-join (cond
                                                                (vector? joined) joined
                                                                (keyword? joined) (build-eql-query joined all-models))))))))
      []
      this)))

(reg-event-db
  :load-eql
  (fn [db [_ {:keys [query model path]}]]
    (.log js/console "maga a query: " (str  [{query (build-eql-query model (:eql db))}]))
    (ajax-post {:url "/api"
                :params [{query (build-eql-query model (:eql db))}]
                :handler #(do
                            (dispatch [:assoc path %])
                            (.log js/console "a valasz: " %))})
    db))


(reg-sub
  :get
  (fn [db [_ the-key]]
    (get db the-key)))

(reg-sub
  :get-in
  (fn [db [_ the-keys]]
    (get-in db the-keys)))




(reg-sub
  :get-eql-model
  (fn [db [_ model]]
    (build-eql-query model (:eql db))))





(dispatch [:add-eql-model {:model :player
                           :query [:player/id :player/name]
                           :ident :game-station/id}])
(defn player []
  [:div "Ez egy player"])


"Miket tudnank osszevonni?
* a queryt nem
* a modelt meg a nevet igazabol megtudnank tenni, de mivan ha nem egyezik?
* a path kell mert el kell menteni valahova
* az ident is kell, tudjam milyen kulcs ala mentsem."

(dispatch [:add-eql-model {:model :game-stations
                           :query [:game-station/id :game-station/name {:game-station/player :player}]
                           :ident :game-station/id}])
(defn game-stations []
  (r/create-class
    {:component-did-mount #(dispatch [:load-eql {:query :all-game-stations
                                                 :model :game-stations
                                                 :path  :game-stations}])
     :reagent-render (fn []
                       [:div
                        [:div "a db-be " (str @(subscribe [:get :game-stations]))]
                        [:div "Ezek a game-stationok"]])}))
      ;[player]])})

(defn reagent-main []
  [:div {:style {:padding "10px" :background "#BBB" :min-height "100vh"}}
   [:h1 "Szia Reagent, ugy hianyoztal!"]
   ;[:h4 "Par adat:" @(subscribe [:get :eql])]
   [:h5 {:style {:background "#999" :padding "10px"}}
    "talan eql-be: "  (str  @(subscribe [:get-eql-model :game-stations]))]
   [game-stations]])









(defmutation make-older [{:gamer/keys [id]}]
  (action [{:keys [state]}]
          (.log js/console "mi a palya mutation" state)
          (swap! state update-in [:gamer/id id :gamer/age] inc)))























(defsc x-player [this {:player/keys [id name]}]
  {:ident :player/id
   :query [:player/name :player/id]}

  (div {:style {:background "#666"}}
       (dom/h4 (str id " : " name))))
;(div (str "hello" gamers))))

(def ui-player (comp/factory x-player))


(defsc x-game-station [this {:game-station/keys [id name player] :as props}]
  {:ident :game-station/id
   :query [:game-station/name :game-station/id {:game-station/player (comp/get-query x-player)}]}

  (div {:style {:background "#666"}}
    (dom/h2  (str "gep neve: " name)
       (str player))
    (dom/h3 (str "player" (ui-player player)))))
    ;(div (str "hello" gamers))))

(def ui-game-station (comp/factory x-game-station))



(def db {:eql {:game-station
                       {:ident :game-station/id
                        :model [:game-station/name :game-station/id {:game-station/player :player}]}
               :player {:ident :game-station/id
                        :model [:player/id :player/name]}}})

(defsc x-game-station-list [this {:game-station-list/keys [game-stations]}]
  {:query [{:game-station-list/game-stations (comp/get-query x-game-station)}]
   :ident (fn [] [:component/id ::game-station-list])
   :initial-state (fn [a] {})}

  (div {:style {:background "#666"}}
       (str "Game-stations" game-stations)
       (map ui-game-station game-stations)))

(def ui-game-station-list (comp/factory x-game-station-list))

(defsc x-root [this {:root/keys [game-station-list]}]
  {:query [{:root/game-station-list (comp/get-query x-game-station-list)}]
   :initial-state (fn [a] {:root/game-station-list {}})}
  (comp/fragment
    ;(str game-stations)
    ;(map ui-game-station game-stations)
    ;(ui-gamer gamer)
    ;(str "A route: " (comp/get-query x-root))
    ;(str "A route: " web-app)
    (ui-game-station-list game-station-list)
    (button {:onClick #(df/load! this :all-game-stations x-game-station
                                 {:target [:component/id ::game-station-list :game-station-list/game-stations]})}
            "Betoltes gomb")))
    ;(div {:style {:background "#CCC"}}
    ;  (map ui-game all-games))))
