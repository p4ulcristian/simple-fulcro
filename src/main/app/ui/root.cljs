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
                 :idents {}
                 :teszt "hello"})

(dispatch [:initialize-db])

(reg-event-fx
  :initialize-db
  ;[(inject-cofx :get-local-storage)]
  (fn [{:keys [db local-storage]} _]
    {:db default-db}))



(defn dissoc-in
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(reg-event-db
  :add-eql
  (fn [db [_ [model-key model]]]
    (assoc-in db [:eql model-key] model)))

(reg-event-db
  :remove-eql
  (fn [db [_ model-key]]
    (dissoc-in db [:eql model-key])))


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
    (vec (remove nil?
                 (reduce
                   (fn [till-now next]
                     (vec (conj till-now (cond
                                           (keyword? next) next
                                           (map? next) (let [[to-join joined] (first next)]
                                                         (if
                                                           (and (keyword? joined) (not (contains? all-models joined)))
                                                           nil
                                                           (assoc {} to-join (cond
                                                                               (vector? joined) joined
                                                                               (keyword? joined) (build-eql-query joined all-models)))))))))
                   []
                   this)))))




(defn normalize-data [db data]
  (let [idents (map :ident (mapv second (:eql db)))]
    (.log js/console "miert"
          (reduce
            (fn [the-db now]
              (let [this-ident (some #(if (contains? now %) % false)
                                     idents)]
                (.log js/console "nezd csak: " this-ident)
                the-db))
            db data))))





(reg-event-db
  :normalize-data
  (fn [db [_ data]]
    (normalize-data db data)))


(reg-event-db
  :load-eql
  (fn [db [_ query model]]
    (.log js/console "maga a query: " (str  [{query (build-eql-query model (:eql db))}]))
    (ajax-post {:url "/api"
                :params [{query (build-eql-query model (:eql db))}]
                :handler #(do
                            (dispatch [:normalize-data (second (first %))])
                            (dispatch [:assoc :teszt %])
                            (.log js/console "a valasz: " %))})
    db))


(reg-sub
  :get
  (fn [db [_ the-key]]
    (get db the-key)))

(reg-sub
  :db
  (fn [db [_ the-key]]
    db))

(reg-sub
  :get-in
  (fn [db [_ the-keys]]
    (get-in db the-keys)))

(reg-sub
  :get-eql
  (fn [db [_ model]]
    (str (build-eql-query :ui/game-station (:eql db)))))


(defn eql [[model-key model] content]
  (r/create-class
    {:component-did-mount #(dispatch [:add-eql [model-key model]])
     :component-will-unmount #(dispatch [:remove-eql model-key])
     :reagent-render (fn [[model-key model] content]
                       content)}))


(def eql-tree []
  {:ui/game [:game/id [:game/name :game/id]]
   :ui/game-station [:game-station/id [:game-station/name :game-station/id
                                       {:game-station/player :ui/player}
                                       {:game-station/game :ui/game}]]})

(defn game []
  [eql [:ui/game {:ident :game/id :query [:game/name :game/id]}]
    [:div {:style {:padding "10px"}} [:h2 "Game:"]]])

(defn player []
  [eql [:ui/player {:ident :game-station/id :query [:player/name :player/id]}]
    [:div {:style {:padding "10px"}} [:h2 "Player: "]]])

(defn game-stations []
  (let [menu? (r/atom :player)]
    (fn []
      [eql [:ui/game-station {:ident :game-station/id
                              :query [:game-station/name :game-station/id
                                      {:game-station/player :ui/player}
                                      {:game-station/game :ui/game}]}]
        [:div
         [:button {:on-click #(reset! menu? :player)} "player"]
         [:button {:on-click #(reset! menu? :game)} "game"]
         [:button {:on-click #(reset! menu? false)} "semmi"]
         [:button {:on-click #(dispatch [:load-eql :all-game-stations :ui/game-station])} "betoltes"]
         (case @menu?
           :player [player]
           :game [game]
           [:div "semmi"])
         [:div {:style {:background "aliceblue" :padding "10px"}}
          [:h2 "Ez a query: "]
          (str @(subscribe [:get-eql :ui/game-station]))]
         [:div {:style {:background "aliceblue" :padding "10px"}}
          [:h2 "Ez a valasz: "]
          (str @(subscribe [:get :teszt]))]]])))


(defn reagent-main []
  [:div {:style {:padding "10px" :background "#BBB" :min-height "100vh"}}
   [:h1 "Szia Reagent, ugy hianyoztal!"]
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
