;;
;; Copyright (C) 2011-2012 Petrozavodsk State University
;;
;; This file is part of Nest.
;;

(ns ru.petrsu.nest.yz.yz-son
  ^{:author "Michail Kryshen, Vyacheslav Dimitrov"
    :doc "Extended SON model. Needed for testing MOM.
         CHANGELOG:
          * added MiddleOU."}
  (:use
    net.kryshen.planter.core)
  (:import
   net.kryshen.planter.core.RefBean
   clojure.lang.Keyword))

(definterface SonBean
  (^String getId [])
  (^String typeName [])
  (^String getDisplayName [])
  (^String briefString [])
  (^String getIconName [])
  (^void addPropertyAccessListener
         [^net.kryshen.planter.core.PropertyAccessListener l])
  (^void removePropertyAccessListener
         [^net.kryshen.planter.core.PropertyAccessListener l])
  (^void addPropertyChangeListener [l])
  (^void removePropertyChangeListener [l]))

(defn- dot-decimal-string
  "Returns the dot-decimal string representation of the address
  specified by byte array or sequence of bytes."
  [address]
  (let [[f & n] (map (partial bit-and 0xFF) address)]
    (apply str f (interleave (repeat \.) n))))

(defn- brief-dot-decimal-string
  "Returns the dot-decimal string representation of the unmasked part
  of the address. Leading dot is added if a part of the address was
  droped."
  [address mask]
  (let [masked (count (take-while (partial == (unchecked-byte 0xFF)) mask))]
    (if (pos? masked)
      (str "." (dot-decimal-string (drop masked address)))
      (dot-decimal-string address))))

(defn- count-bits [mask]
  (reduce #(+ %1 (Integer/bitCount (bit-and 0xFF %2))) 0 mask))

(defn- colon-hex-string
  [address]
  (let [[f & n] (map (partial bit-and 0xFF) address)]
    (apply str (format "%02X" f) (map (partial format ":%02X") n))))

(defmixin SonElement
  [^String name
   ^String description]
  Object
  (toString [^SonBean e]
    (let [display-name (.getDisplayName e)]
      (if (or (nil? display-name) (empty? display-name))
        (.typeName e)
        (str (.typeName e) \space display-name))))
  Comparable
  (compareTo [_ _]
    0)
  SonBean
  (getId [e]
    (str (bean-id e)))
  (typeName [e]
    (.getSimpleName (class e)))
  (getDisplayName [e]
    (get-property e :name))
  (briefString [^SonBean e]
    (.getDisplayName e))
  (getIconName [e]
    nil)
  (addPropertyAccessListener [^RefBean e l]
    (dosync
     (alter (:access-listeners (.internalState e)) conj l)))
  (removePropertyAccessListener [^RefBean e l]
    (dosync
     (alter (:access-listeners (.internalState e))
            (partial remove (partial = l)))))
  ;; TODO: implement
  (addPropertyChangeListener [_ _])
  (removePropertyChangeListener [_ _]))

(defbean SON
  [^Device rootDevice
   ^CompositeOU rootOU
   ^Building* ^{:init #{}} buildings]
  SonElement)

;; Occupancy

(defbean Occupancy
  [^SimpleOU ^{:bind occupancies} OU
   ^Room ^{:bind occupancies} room
   ^Device* ^{:bind occupancy :init #{}} devices]
  SonElement)

;; Network

(defmixin NetworkElement []
  SonElement)

(defbean Device
  [^Occupancy ^{:bind devices} occupancy
   ^LinkInterface* ^{:bind device :init #{}} linkInterfaces
   ^boolean ^{:init false} forwarding]
  NetworkElement
  (getIconName [e]
    (if (get-property e :forwarding)
      "son-device-router.png"
      "son-device.png")))

(defmixin LinkInterface
  [^Keyword ^{:init :up} state
   ^Keyword ^{:init :duplex} mode
   ^LinkInterface ^{:bind link} link
   ^NetworkInterface* ^{:bind linkInterface :init #{}} networkInterfaces
   ^Device ^{:bind linkInterfaces} device]
  NetworkElement
  (getIconName [e]
    "son-interface-link.png"))

(defbean UnknownLinkInterface []
  LinkInterface)

(defbean EthernetInterface
  [^bytes MACAddress]
  LinkInterface
  (getDisplayName [e]
    (or (get-property e :name)
        (colon-hex-string (get-property e :MACAddress))))
  (briefString [e]
    (get-property e :name)))

(defbean VLANInterface
  [^Integer vlanId
   ^LinkInterface* ^{:init #{}} linkInterfaces]
  LinkInterface
  (getDisplayName [e]
    (or (get-property e :name)
        (format "#%d" (get-property e :vlanId)))))

(defmixin NetworkInterface
  [^Boolean ^{:init true} active
   ^bytes inetAddress
   ^LinkInterface ^{:bind networkInterfaces} linkInterface
   ^Network ^{:bind networkInterfaces} network]
  NetworkElement
  (getIconName [e]
    "son-interface.png")
  (getDisplayName [e]
    (or (get-property e :name)
        (dot-decimal-string (get-property e :inetAddress))))
  (briefString [e]
    (brief-dot-decimal-string
     (get-property e :inetAddress)
     (-> e (get-property :network) (get-property :mask)))))

(defbean UnknownNetworkInterface []
  NetworkInterface)

(defbean IPv4Interface []
  NetworkInterface)

(defmixin Network
  [^NetworkInterface* ^{:bind network :init #{}} networkInterfaces]
  NetworkElement
  (getIconName [e]
    "son-network.png"))

(defbean UnknownNetwork []
  Network)

(defbean IPNetwork
  [^bytes mask
   ^bytes address]
  Network
  (getDisplayName [e]
    (or (get-property e :name)
        (str (dot-decimal-string (get-property e :address))
             "/"
             (count-bits (get-property e :mask))))))

;; Organizational

(defmixin OrganizationalElement []
  SonElement)

(defmixin AbstractOU
  [^CompositeOU ^{:bind OUs} parent]
  OrganizationalElement)

(defbean CompositeOU
  [^AbstractOU* ^{:bind parent :init #{}} OUs]
  AbstractOU
  (getIconName [e]
    "son-ou-composite.png"))

(defbean SimpleOU
  [^Occupancy* ^{:bind OU :init #{}} occupancies]
  AbstractOU
  (getIconName [e]
    "son-ou-simple.png"))

(defbean MiddleOU
  [^Room ^{:bind mou} room]
  AbstractOU
  (getIconName [e]
    "son-ou-simple.png"))

;; Spatial

(defmixin SpatialElement []
  SonElement)

(defbean Room
  [^String number
   ^Floor ^{:bind rooms} floor
   ^Occupancy* ^{:bind room :init #{}} occupancies 
   ^MiddleOU ^{:bind room} mou]
  SpatialElement
  (getIconName [e]
    "son-room.png")
  (getDisplayName [e]
    (or (get-property e :name)
        (get-property e :number))))

(defbean Floor
  [^Integer number
   ^Building ^{:bind floors} building
   ^Room* ^{:bind floor :init #{}} rooms]
  SpatialElement
  (getIconName [e]
    "son-floor.png")
  (getDisplayName [e]
    (or (get-property e :name)
        (str (get-property e :number)))))

(defbean Building
  [^String address
   ^Floor* ^{:bind building :init #{}} floors]
  SpatialElement
  (getIconName [e]
    "son-building.png"))

(generate-beans)
