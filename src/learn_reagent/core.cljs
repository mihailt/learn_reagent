(ns ^:figwheel-always learn-reagent.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :refer-macros [defroute]]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(enable-console-print!)

(println "Aargh!!!")

; config
(def board-size 16)
(def alpha-chars (mapv char (range 97 123)))
(defn generate-symbols [] (shuffle (flatten (repeat 2 (take (/ board-size 2) (shuffle alpha-chars))))))

; state
(defonce state (atom {:started false
                      :cards []
                      :last-symbol nil}))


(defn generate-card [symb] (atom {:symbol symb :visible false :matched false}))
(defn generate-cards [] (mapv generate-card (generate-symbols)))

(defn matched? [card] (:matched @card))
(defn visible? [card] (:visible @card))

(defn revealed-cards-count [] (count (filter visible? (:cards @state))))
(defn matched-cards-count [] (count (filter matched? (:cards @state))))

(defn hide-nonmatch! []
  (doseq [card-state (:cards @state)]
    (swap! card-state assoc :visible false))
  (swap! state assoc :last-symbol ""))

(defn mark-match! [symbol]
  (doseq [matched-card (filterv #(= (:symbol @%) symbol)
                                (:cards @state))]
    (swap! matched-card assoc :matched true)))

(defn reveal-card! [card-state] (swap! card-state assoc :visible true))

(defn start-game [] (swap! state assoc :started true :cards (generate-cards)))

(defn card [card-state]
  (letfn [(handle-card-click! [event]
            (when-not (@card-state :matched)
              ;; pair of cards was revealed, now let's go for another pair step
              (when (= (revealed-cards-count) 2)
                (hide-nonmatch!))

              ;; reveal next card in step
              (reveal-card! card-state)

              ;; if 2 of cards are revealed, we have to check parity
              (when (and (= (revealed-cards-count) 2)
                         (= (:last-symbol @state)
                            (:symbol @card-state)))
                (mark-match! (:symbol @card-state)))

              ;; let's remember last symbol to make comparison in subsequent steps
              (swap! state assoc :last-symbol (:symbol @card-state))
              (when (= (matched-cards-count) board-size)
                (swap! state assoc :started false))))]

    [:div.card
     {:onClick handle-card-click!
      :key     (.random js/Math)
      :class   (if (@card-state :matched)
                 "card-matched"
                 "card")}

     [:span.card-value
      {:class (if (@card-state :visible)
                "card-value"
                "card-value-hidden")}
      (:symbol @card-state)]]))

(defn home-page []
  [:div.memory
   [:h2 "Memory Game"]
   [:div.status
    (if (= (matched-cards-count) board-size)
       "Game is finished, congratulations !")]

   [:br]
   (if-not (:started @state)
        [:button.button {:onClick start-game} "Start game"])

   [:div.board
    (doall (for [card-state (:cards @state)]
             (card card-state)))]])

; routes
(defn current-page []
  [:div [(session/get :current-page)]])

(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen EventType/NAVIGATE
                   (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

; init
(hook-browser-navigation!)
(mount-root)


(defn on-js-reload []
    (mount-root)
)

