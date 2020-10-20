(ns app.server-components.messenger
  (:require [clj-http.client :as client]))
;(:require))



(defn template [type content]
  {:message {:attachment
             {:type "template",
              :payload
                    (merge
                      {:template_type type,}
                      content)}}})

(defn generic-template []
  (template "generic"
            {:elements [{:title     "Udvozlegy!",
                         :image_url "https://portenoire.com/wp-content/uploads/2019/07/2019_07_7-Good-Reasons-Not-to-Use-Balayage-for-Highlights-1200x628-1080x628.jpg",
                         :subtitle  "Akarsz zurabb hajat?"
                         :buttons   [{:type  "web_url",
                                      :url   "https://petersfancybrownhats.com",
                                      :title "Igen!!!"}
                                     {:type    "postback",
                                      :title   "Nem :(",
                                      :payload "no"}]}]}))

(defn opengraph-template []
  (template "open_graph" {:elements
                          [{:url "https://open.spotify.com/track/7GhIk7Il098yCjg4BQjzvb",
                            :buttons
                                 [{:type  "web_url",
                                   :url
                                          "https://en.wikipedia.org/wiki/Rickrolling",
                                   :title "View More"}]}]}))


(defn button-template []
  (template "button" {:text          "Miben tudok segiteni? :)"
                      :buttons       [{:type  "postback",
                                       :title "Idopontot akarok!",
                                       :payload
                                              "no"}
                                      {:type  "postback",
                                       :title "Merre van az uzlet?",
                                       :payload
                                              "no"}
                                      {:type  "postback",
                                       :title "Veled akarok beszelni!",
                                       :payload
                                              "no"}]}))













(def access-token "EAAEjz3jLNwwBALdlvmpZBqPkZC4CtPRCikHSpkhZAftceFS68zAbfEx8jGChWIjYKCC7ujWJ2HI65QaBqHOiDVPdux6tZBDMnItfAHJGoM5XeT5DdAlJopjy5qQWSX6tCGVQmzQydwH68HIsXZBbue6s7C4jO8klq0SwCEzl48VYT3F0kimD8")

(def verify-token (str "color-me-crazy-verify-token"))


(def send-api-uri "https://graph.facebook.com/v2.6/me/messages")
(def settings-api-uri "https://graph.facebook.com/v8.0/me/custom_user_settings")


(defn with-token [url]
  (str url "?access_token=" access-token))

(defn persistent-menu []
  {:persistent_menu [{:locale                  "default",
                      :composer_input_disabled false,
                      :call_to_actions
                                               [{:type    "postback",
                                                 :title   "Talk to an agent",
                                                 :payload "CARE_HELP"}
                                                {:type    "postback",
                                                 :title   "Outfit suggestions",
                                                 :payload "CURATION"}
                                                {:type                 "web_url",
                                                 :title                "Shop now",
                                                 :url
                                                                       "https://www.originalcoastclothing.com/",
                                                 :webview_height_ratio "full"}]}]})

(defn quick-replies []
  {:messaging_type "RESPONSE",
   :message {:text "Pick a color:",
             :quick_replies
                   [{:content_type "text",
                     :title "button",
                     :payload "<POSTBACK_PAYLOAD>",
                     :image_url
                                   "http://example.com/img/red.png"}
                    {:content_type "text",
                     :title "rick",
                     :payload "<POSTBACK_PAYLOAD>",
                     :image_url "http://example.com/img/green.png"}
                    {:content_type "text",
                     :title "generic",
                     :payload "<POSTBACK_PAYLOAD>",
                     :image_url "http://example.com/img/green.png"}]}})

(defn post-with-token [url content]
  (try
    (client/post (with-token url)
                 content)
    (catch Exception e (println "hiba: " (:headers (.getData e))))))

(defn change-user-settings [sender-id response]
  (let [new-response (merge
                       {:psid sender-id}
                       response)]

    (post-with-token settings-api-uri {:form-params  new-response
                                       :content-type :json})))

(defn call-send-api [sender-id response]
  (let [new-response (merge
                       {:recipient {:id sender-id}}
                       response)]

    (post-with-token send-api-uri {:form-params  new-response
                                   :content-type :json})))



(defn handle-message
  "This handles the incoming messages. what we do, and when we do it."
  [sender-id {:keys [mid text]}]
  (println text)
  (case text
    "button" (call-send-api sender-id (button-template))
    "generic" (call-send-api sender-id (generic-template))
    "rick" (call-send-api sender-id (opengraph-template))
    "majom" (call-send-api sender-id {:message {:text "Te vagy a majom"}})
    (call-send-api sender-id (quick-replies))))
    ;(call-send-api sender-id {:message {:text "Ezt meg nem tudom hogy kell!"}})))
  ;(call-send-api sender-id (opengraph-template))
  ;(change-user-settings sender-id (persistent-menu)))


(defn handle-postback [sender-id received-postback])





(defn webhook-post [req]
  (let [body (:body req)
        {:keys [object entry]} body]
    (println "Messenger-bot webhook: " object)
    (if (= "page" object)
      (doseq [one-entry entry]
        (let [webhook-event (first (:messaging one-entry))
              {:keys [sender message postback]} webhook-event
              sender-id (:id sender)]
          (println sender-id " " body)
          (if message (handle-message sender-id message)
                      (handle-postback sender-id postback))))))


  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "hello"})

(defn get-challenge [req]
  (let [challenge (get (:params req) "hub.challenge")]
    challenge))

(defn webhook-get [req]
  (println "Messenger-bot webhook verification" req)
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (get-challenge req)})
