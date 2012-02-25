;;
;; Copyright 2011-2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
;;
;; This file is part of YZ.
;;
;; YZ is free software: you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License version 3
;; only, as published by the Free Software Foundation.
;;
;; YZ is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Lesser General Public License for more details.
;;
;; You should have received a copy of the GNU Lesser General Public
;; License along with YZ.  If not, see
;; <http://www.gnu.org/licenses/>.
;;

(ns ru.petrsu.nest.yz.queries.nest-queries
  ^{:author "Vyacheslav Dimitrov"
    :doc "Queries from NEST project."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc]
            [ru.petrsu.nest.yz.queries.bd :as bd]))


;; Define fixtures.

(use-fixtures :once (tc/setup-son bd/son))


(def address-info-queries
  ^{:doc "Queries which are used with AddressInfo Nestling."}
  ["d#(ni.inetAddress = @(ip2b \"192.168.112.48\"))"
   "ni[id @(ip &.inetAddress) @(ip &.inetAddress)]#(d.id = \"014b0f92-0322-4ff4-a5e6-dcc8a998a7b8\")"
   "ei[id @(mac &.MACAddress)]#(d.id = \"014b0f92-0322-4ff4-a5e6-dcc8a998a7b8\")"
   "network#(device.id = \"014b0f92-0322-4ff4-a5e6-dcc8a998a7b8\")"
   "d#(id = \"014b0f92-0322-4ff4-a5e6-dcc8a998a7b8\") (network.ni[@(ip &.inetAddress)]#(device.forwarding=true) (d#(forwarding=true)))"
   "li#(ni.inetAddress = @(ip2b \"192.168.112.62\"))"])


(def enlivener-queries
  ^{:doc "Queries which are used with AddressInfo Nestling."}
    ["b#(name = \"GK\")"
     "f#(number = nil && b.id = \"94dd283f-7cbc-46f5-a50f-32193272a8f2\")"
     "r#(number = nil && f.id = \"cc642ea0-7de3-41c8-852e-b79b2793fb42\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"9f72182e-74bf-486c-9297-11f46c309c56\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"GK\")"
     "f#(number = nil && b.id = \"94dd283f-7cbc-46f5-a50f-32193272a8f2\")"
     "r#(number = nil && f.id = \"cc642ea0-7de3-41c8-852e-b79b2793fb42\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"РЦНИТ\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "aou#(name = \"Отдел телекоммуникаций\" && cou.id = \"a3651bb6-072a-436e-94f1-13dd0510a659\")"
     "o#(r.id = \"9f72182e-74bf-486c-9297-11f46c309c56\" && sou.id = \"42dcee3c-7cf9-4958-ae55-a8c3fce1356e\")"
     "b#(name = \"GK\")"
     "f#(number = nil && b.id = \"94dd283f-7cbc-46f5-a50f-32193272a8f2\")"
     "r#(number = nil && f.id = \"cc642ea0-7de3-41c8-852e-b79b2793fb42\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Private\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"9f72182e-74bf-486c-9297-11f46c309c56\" && sou.id = \"81d01779-7543-4f1c-8186-b423ec5c3a30\")"
     "b#(name = \"GK\")"
     "f#(number = nil && b.id = \"94dd283f-7cbc-46f5-a50f-32193272a8f2\")"
     "r#(number = nil && f.id = \"cc642ea0-7de3-41c8-852e-b79b2793fb42\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"РЦНИТ\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "aou#(name = \"Отдел Web-технологий\" && cou.id = \"a3651bb6-072a-436e-94f1-13dd0510a659\")"
     "o#(r.id = \"9f72182e-74bf-486c-9297-11f46c309c56\" && sou.id = \"7771b5ab-0133-4e7d-81ac-cda2735d9cc0\")"
     "b#(name = \"GK\")"
     "f#(number = 1 && b.id = \"94dd283f-7cbc-46f5-a50f-32193272a8f2\")"
     "r#(number = \"148\" && f.id = \"a40156dd-9244-4f19-9782-dd585a768852\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Математический факультет\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "aou#(name = \"Кафедра ИМО\" && cou.id = \"9aafe647-b31f-4833-9aac-f7cd423034b3\")"
     "o#(r.id = \"c5a24f60-2a92-48dd-b8a6-c29ab97a8a58\" && sou.id = \"070d95a8-acfb-4a2b-bacc-0a217114381b\")"
     "b#(name = \"GK\")"
     "f#(number = 2 && b.id = \"94dd283f-7cbc-46f5-a50f-32193272a8f2\")"
     "r#(number = \"215\" && f.id = \"b8c7cf2a-afde-4e20-bc9b-1711bfa83b4d\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Математический факультет\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "aou#(name = \"Кафедра ИМО\" && cou.id = \"9aafe647-b31f-4833-9aac-f7cd423034b3\")"
     "o#(r.id = \"7117942a-eeae-4e8f-9ccf-6f663928a2b5\" && sou.id = \"070d95a8-acfb-4a2b-bacc-0a217114381b\")"
     "b#(name = \"GK\")"
     "f#(number = nil && b.id = \"94dd283f-7cbc-46f5-a50f-32193272a8f2\")"
     "r#(number = nil && f.id = \"cc642ea0-7de3-41c8-852e-b79b2793fb42\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"9f72182e-74bf-486c-9297-11f46c309c56\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"GK\")"
     "f#(number = 1 && b.id = \"94dd283f-7cbc-46f5-a50f-32193272a8f2\")"
     "r#(number = \"142\" && f.id = \"a40156dd-9244-4f19-9782-dd585a768852\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"РЦНИТ\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "aou#(name = \"Отдел Web-технологий\" && cou.id = \"a3651bb6-072a-436e-94f1-13dd0510a659\")"
     "o#(r.id = \"e7e824df-1a1f-4378-b3b9-2dde12d0918b\" && sou.id = \"7771b5ab-0133-4e7d-81ac-cda2735d9cc0\")"
     "b#(name = \"UK2\")"
     "f#(number = nil && b.id = \"4853055b-e026-4a37-868d-62f2bb571214\")"
     "r#(number = nil && f.id = \"0325d9aa-6ef7-4e89-9d9a-bf665b6f742e\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"f55922d4-c8bf-4e9c-94e8-1e3fe1c5db5f\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"UK3\")"
     "f#(number = nil && b.id = \"c5b036eb-9582-4d69-89bd-6c2ba85a19c7\")"
     "r#(number = nil && f.id = \"df03ecb0-32a1-4b1b-8257-a93ea3c44cf9\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"b9d2b12b-1bff-4a0f-8d43-60e10d441a18\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"UK4\")"
     "f#(number = nil && b.id = \"82a426eb-a381-4bc8-be65-ae4321817478\")"
     "r#(number = nil && f.id = \"ed388e7b-ea5c-41b8-9b1f-490fe68d0afe\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"8e245966-24fc-450b-a388-fdca2b0739db\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"UK5\")"
     "f#(number = nil && b.id = \"48749980-25cb-433a-bdfe-5528ca51311b\")"
     "r#(number = nil && f.id = \"96636c8f-7f9b-450e-b0d4-1017268731b8\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"006f00a8-8f0c-4b32-944d-b63b8ec53755\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"UK6\")"
     "f#(number = nil && b.id = \"679e44e0-8dfb-4f13-abb4-3766b125c220\")"
     "r#(number = nil && f.id = \"6bf26235-390a-432a-b6d5-1e1bd1867bfe\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"bd300686-4990-4463-b619-6326a7be93c2\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"UK8\")"
     "f#(number = nil && b.id = \"aa300257-3bac-46b9-b52d-98ae43fafe5a\")"
     "r#(number = nil && f.id = \"db649e60-3895-4144-ad79-50a316fe4cd7\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"0a64ed37-adbd-4745-8a04-c58496ed5a80\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"UK8\")"
     "f#(number = nil && b.id = \"aa300257-3bac-46b9-b52d-98ae43fafe5a\")"
     "r#(number = nil && f.id = \"db649e60-3895-4144-ad79-50a316fe4cd7\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"0a64ed37-adbd-4745-8a04-c58496ed5a80\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"UK9\")"
     "f#(number = nil && b.id = \"c9f221f7-d9a3-4f4e-95b8-35d92537c1f0\")"
     "r#(number = nil && f.id = \"a9594f4c-ba9d-4d38-917f-1d4872a95807\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Public\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"e067fc71-df26-416f-afa0-6c09953b9480\" && sou.id = \"293851ce-c331-47a6-9aaa-057ba0788450\")"
     "b#(name = \"UK9\")"
     "f#(number = nil && b.id = \"c9f221f7-d9a3-4f4e-95b8-35d92537c1f0\")"
     "r#(number = nil && f.id = \"a9594f4c-ba9d-4d38-917f-1d4872a95807\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Гостиница\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"e067fc71-df26-416f-afa0-6c09953b9480\" && sou.id = \"2ecaafff-88f6-439f-8ffa-4800b39dc525\")"
     "b#(name = \"UK9\")"
     "f#(number = 3 && b.id = \"c9f221f7-d9a3-4f4e-95b8-35d92537c1f0\")"
     "r#(number = nil && f.id = \"cd4a8030-7fd4-44f6-9961-65c7a366b15c\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Metso\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"a6bce25b-51c3-4672-93b3-8666701aec0b\" && sou.id = \"5ead1394-ff85-4496-a190-a75c630666e1\")"
     "b#(name = \"MFK\")"
     "f#(number = nil && b.id = \"f3dbb422-4160-4ae7-a5f1-0d2e22fb4477\")"
     "r#(number = nil && f.id = \"2696415c-1313-4d91-b907-8984365690d1\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Медицинский факультет\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"d64c15dd-9b7a-4c61-9a7f-42d895e4491a\" && sou.id = \"a5b7876f-ccda-472a-b8f3-7b43bf90f6ad\")"
     "b#(name = \"TK\")"
     "f#(number = nil && b.id = \"9b8078d6-9fb0-4c61-aa32-06be4942e7f8\")"
     "r#(number = nil && f.id = \"7df802aa-a265-41ea-aea3-adb1f28f825f\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Медицинский факультет\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"7152312d-f873-4920-ba48-64999d199f3b\" && sou.id = \"a5b7876f-ccda-472a-b8f3-7b43bf90f6ad\")"
     "b#(name = \"RB\")"
     "f#(number = nil && b.id = \"c29c4a2e-2cb4-4e91-93f1-901cd6b489dc\")"
     "r#(number = nil && f.id = \"c981af55-7886-4b6c-8409-c3cb8f7389cd\")"
     "aou#(name = \"ПетрГУ\" && cou.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"
     "aou#(name = \"Медицинский факультет\" && cou.id = \"4b946015-daf1-428e-b79b-4071853fb4ec\")"
     "o#(r.id = \"e177d321-5158-4da4-85a9-33ca7249096d\" && sou.id = \"a5b7876f-ccda-472a-b8f3-7b43bf90f6ad\")"
     "li#(description != nil && description != \"\") (n (d))"])
 

(def tree-queries
  ^{:doc "Queries which are used for building SON tree."}
  ["n#(sou.id = \"c29c4a2e-2cb4-4e91-93f1-901cd6b489dc\")"
   "d#(sou.id = \"9b8078d6-9fb0-4c61-aa32-06be4942e7f8\" && n.id = \"a5b7876f-ccda-472a-b8f3-7b43bf90f6ad\")"
   "n#(r.id = \"a6bce25b-51c3-4672-93b3-8666701aec0b\")" 
   "d#(r.id = \"e067fc71-df26-416f-afa0-6c09953b9480\" && n.id = \"104a5137-4b1f-4acc-a1af-ea009970a72d\")"])


(defmacro is-list?
  [queries]
  (concat
    `(do)
    (for [query (eval queries)]
      `(deftest ~(gensym)
                (is (tc/rows-query ~query))))))


(is-list? address-info-queries)
(is-list? enlivener-queries)
(is-list? tree-queries)

(def address-info-queries-hql
  ^{:doc "HQL queries which are used with AddressInfo Nestling."}
  ["select d, ni.inetAddress from Device as d join d.linkInterfaces as li join li.networkInterfaces as ni where ni.inetAddress = null" 
   "select ni.id, ni.inetAddress from NetworkInterface ni where ni.linkInterface.device.id=1"
   "select ei.id, ei.MACAddress from EthernetInterface ei where ei.device.id=2"
   "select n from Network n join n.networkInterfaces ni where ni.linkInterface.device.id=3"
   "select d, ni2.inetAddress, ni2.linkInterface.device.forwarding from Device d join d.linkInterfaces li join li.networkInterfaces ni 
   join ni.network.networkInterfaces ni2 where d.id=4 and ni2.linkInterface.device.forwarding=true"
   "select li from LinkInterface li join li.networkInterfaces ni where ni.inetAddress=null"])


(def enlivener-queries-hql
  ^{:doc "HQL queries which are used with AddressInfo Nestling."}
    ["select b from Building b where b.name='GK'"
     "select f from Floor f join f.building b where f.number = null and b.id=1"
     "select r from Room r join r.floor f where r.number = null and f.id=2"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=1 and o.OU.id=2"
     "select b from Building as b where b.name='GK'"
     "select f from Floor f join f.building b where f.number = null and b.id=3"
     "select r from Room r join r.floor f where r.number = null and f.id=4"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='РЦНИТ' and parent.id=2"
     "select aou from AbstractOU aou where name='Отдел телекоммуникаций' and parent.id=3"
     "select o from Occupancy o where o.room.id=1 and o.OU.id=2"
     "select b from Building as b where b.name='GK'"
     "select f from Floor f join f.building b where f.number = null and b.id=5"
     "select r from Room r join r.floor f where r.number = null and f.id=6"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Private' and parent.id=4"
     "select o from Occupancy o where o.room.id=3 and o.OU.id=4"
     "select b from Building as b where b.name='GK'"
     "select f from Floor f join f.building b where f.number = null and b.id=7"
     "select r from Room r join r.floor f where r.number = null and f.id=8"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='РЦНИТ' and parent.id=2"
     "select aou from AbstractOU aou where name='Отдел Web-технологий' and parent.id=3"
     "select o from Occupancy o where o.room.id=5 and o.OU.id=6"
     "select b from Building as b where b.name='GK'"
     "select f from Floor f join f.building b where f.number = null and b.id=9"
     "select r from Room r join r.floor f where r.number = null and f.id=10"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Математический факультет' and parent.id=2"
     "select aou from AbstractOU aou where name='Кафедра ИМО' and parent.id=3"
     "select o from Occupancy o where o.room.id=7 and o.OU.id=8"
     "select b from Building as b where b.name='GK'"
     "select f from Floor f join f.building b where f.number = null and b.id=11"
     "select r from Room r join r.floor f where r.number = null and f.id=12"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Математический факультет' and parent.id=2"
     "select aou from AbstractOU aou where name='Кафедра ИМО' and parent.id=3"
     "select o from Occupancy o where o.room.id=9 and o.OU.id=10"
     "select b from Building as b where b.name='GK'"
     "select f from Floor f join f.building b where f.number = null and b.id=13"
     "select r from Room r join r.floor f where r.number = null and f.id=14"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=11 and o.OU.id=12"
     "select b from Building as b where b.name='GK'"
     "select f from Floor f join f.building b where f.number = null and b.id=15"
     "select r from Room r join r.floor f where r.number = null and f.id=16"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='РЦНИТ' and parent.id=2"
     "select aou from AbstractOU aou where name='Отдел Web-технологий' and parent.id=3"
     "select o from Occupancy o where o.room.id=13 and o.OU.id=14"
     "select b from Building as b where b.name='UK2'"
     "select f from Floor f join f.building b where f.number = null and b.id=17"
     "select r from Room r join r.floor f where r.number = null and f.id=18"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=15 and o.OU.id=16"
     "select b from Building as b where b.name='UK3'"
     "select f from Floor f join f.building b where f.number = null and b.id=19"
     "select r from Room r join r.floor f where r.number = null and f.id=20"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=17 and o.OU.id=18"
     "select b from Building as b where b.name='UK4'"
     "select f from Floor f join f.building b where f.number = null and b.id=21"
     "select r from Room r join r.floor f where r.number = null and f.id=22"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=19 and o.OU.id=20"
     "select b from Building as b where b.name='UK5'"
     "select f from Floor f join f.building b where f.number = null and b.id=23"
     "select r from Room r join r.floor f where r.number = null and f.id=24"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=21 and o.OU.id=22"
     "select b from Building as b where b.name='UK6'"
     "select f from Floor f join f.building b where f.number = null and b.id=25"
     "select r from Room r join r.floor f where r.number = null and f.id=26"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=23 and o.OU.id=24"
     "select b from Building as b where b.name='UK8'"
     "select f from Floor f join f.building b where f.number = null and b.id=27"
     "select r from Room r join r.floor f where r.number = null and f.id=28"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=25 and o.OU.id=26"
     "select b from Building as b where b.name='UK8'"
     "select f from Floor f join f.building b where f.number = null and b.id=29"
     "select r from Room r join r.floor f where r.number = null and f.id=30"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=27 and o.OU.id=28"
     "select b from Building as b where b.name='UK9'"
     "select f from Floor f join f.building b where f.number = null and b.id=31"
     "select r from Room r join r.floor f where r.number = null and f.id=32"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Public' and parent.id=2"
     "select o from Occupancy o where o.room.id=29 and o.OU.id=30"
     "select b from Building as b where b.name='UK9'"
     "select f from Floor f join f.building b where f.number = null and b.id=33"
     "select r from Room r join r.floor f where r.number = null and f.id=34"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Гостиница' and parent.id=2"
     "select o from Occupancy o where o.room.id=31 and o.OU.id=32"
     "select b from Building as b where b.name='UK9'"
     "select f from Floor f join f.building b where f.number = null and b.id=35"
     "select r from Room r join r.floor f where r.number = null and f.id=36"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Metso' and parent.id=2"
     "select o from Occupancy o where o.room.id=33 and o.OU.id=34"
     "select b from Building as b where b.name='MFK'"
     "select f from Floor f join f.building b where f.number = null and b.id=37"
     "select r from Room r join r.floor f where r.number = null and f.id=38"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Медицинский факультет' and parent.id=2"
     "select o from Occupancy o where o.room.id=35 and o.OU.id=36"
     "select b from Building as b where b.name='TK'"
     "select f from Floor f join f.building b where f.number = null and b.id=39"
     "select r from Room r join r.floor f where r.number = null and f.id=40"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Медицинский факультет' and parent.id=2"
     "select o from Occupancy o where o.room.id=37 and o.OU.id=38"
     "select b from Building as b where b.name='RB'"
     "select f from Floor f join f.building b where f.number = null and b.id=41"
     "select r from Room r join r.floor f where r.number = null and f.id=42"
     "select aou from AbstractOU aou where name='ПетрГУ' and parent.id=1"
     "select aou from AbstractOU aou where name='Медицинский факультет' and parent.id=2"
     "select o from Occupancy o where o.room.id=39 and o.OU.id=40"
     "select li, ni.network, d from LinkInterface li join li.networkInterfaces ni join ni.network n join n.networkInterfaces 
     ni2 join ni2.linkInterface.device d where li.description != null and li.description != ''"])


(def tree-queries-hql
  ^{:doc "Queries which are used for building SON tree."}
  ["select distinct n from Network n join n.networkInterfaces nis 
   where nis.linkInterface.device.occupancy.OU.id = 1"
   "select distinct d from Device d join d.linkInterfaces li join li.networkInterfaces nis
   where d.occupancy.OU.id=2 and nis.network.id=3"
   "select distinct n from Network n join n.networkInterfaces nis 
   where nis.linkInterface.device.occupancy.room.id = 4"
   "select distinct d from Device d join d.linkInterfaces li join li.networkInterfaces nis
   where d.occupancy.room.id=5 and nis.network.id=6"])


(def address-info-queries-jpa
  ^{:doc "JPA queries which are used with AddressInfo Nestling."}
  ["d#(ni.inetAddress = @(ip2b \"192.168.112.48\"))"
   "ni[id @(ip &.inetAddress) @(ip &.inetAddress)]#(d.id = 1)"
   "ei[id @(mac &.MACAddress)]#(d.id = 2)"
   "network#(device.id = 3)"
   "d#(id = 4) (network.ni[@(ip &.inetAddress)]#(device.forwarding=true) (d#(forwarding=true)))"
   "li#(ni.inetAddress = @(ip2b \"192.168.112.62\"))"])


(def enlivener-queries-jpa
  ^{:doc "JPA queries which are used with AddressInfo Nestling."}
    ["b#(name = \"GK\")"
     "f#(number = nil && b.id = 1)"
     "r#(number = nil && f.id = 2)"
     "aou#(name = \"ПетрГУ\" && cou.id = 1)"
     "aou#(name = \"Public\" && cou.id = 2)"
     "o#(r.id = 1 && sou.id = 2)"
     "b#(name = \"GK\")"
     "f#(number = nil && b.id = 3)"
     "r#(number = nil && f.id = 4)"
     "aou#(name = \"ПетрГУ\" && cou.id = 5)"
     "aou#(name = \"РЦНИТ\" && cou.id = 6)"
     "aou#(name = \"Отдел телекоммуникаций\" && cou.id = 7)"
     "o#(r.id = 1 && sou.id = 2)"
     "b#(name = \"GK\")"
     "f#(number = nil && b.id = 5)"
     "r#(number = nil && f.id = 6)"
     "aou#(name = \"ПетрГУ\" && cou.id = 8)"
     "aou#(name = \"Private\" && cou.id = 9)"
     "o#(r.id = 3 && sou.id = 4)"
     "b#(name = \"GK\")"
     "f#(number = nil && b.id = 7)"
     "r#(number = nil && f.id = 8)"
     "aou#(name = \"ПетрГУ\" && cou.id = 10)"
     "aou#(name = \"РЦНИТ\" && cou.id = 11)"
     "aou#(name = \"Отдел Web-технологий\" && cou.id = 12)"
     "o#(r.id = 5 && sou.id = 6)"
     "b#(name = \"GK\")"
     "f#(number = 1 && b.id = 9)"
     "r#(number = \"148\" && f.id = 10)"
     "aou#(name = \"ПетрГУ\" && cou.id = 13)"
     "aou#(name = \"Математический факультет\" && cou.id = 14)"
     "aou#(name = \"Кафедра ИМО\" && cou.id = 15)"
     "o#(r.id = 7 && sou.id = 8)"
     "b#(name = \"GK\")"
     "f#(number = 2 && b.id = 11)"
     "r#(number = \"215\" && f.id = 12)"
     "aou#(name = \"ПетрГУ\" && cou.id = 13)"
     "aou#(name = \"Математический факультет\" && cou.id = 14)"
     "aou#(name = \"Кафедра ИМО\" && cou.id = 15)"
     "o#(r.id = 9 && sou.id = 10)"
     "b#(name = \"GK\")"
     "f#(number = nil && b.id = 13)"
     "r#(number = nil && f.id = 14)"
     "aou#(name = \"ПетрГУ\" && cou.id = 15)"
     "aou#(name = \"Public\" && cou.id = 16)"
     "o#(r.id = 11 && sou.id = 12)"
     "b#(name = \"GK\")"
     "f#(number = 1 && b.id = 15)"
     "r#(number = \"142\" && f.id = 16)"
     "aou#(name = \"ПетрГУ\" && cou.id = 17)"
     "aou#(name = \"РЦНИТ\" && cou.id = 18)"
     "aou#(name = \"Отдел Web-технологий\" && cou.id = 19)"
     "o#(r.id = 13 && sou.id = 14)"
     "b#(name = \"UK2\")"
     "f#(number = nil && b.id = 17)"
     "r#(number = nil && f.id = 18)"
     "aou#(name = \"ПетрГУ\" && cou.id = 19)"
     "aou#(name = \"Public\" && cou.id = 20)"
     "o#(r.id = 15 && sou.id = 16)"
     "b#(name = \"UK3\")"
     "f#(number = nil && b.id = 19)"
     "r#(number = nil && f.id = 20)"
     "aou#(name = \"ПетрГУ\" && cou.id = 21)"
     "aou#(name = \"Public\" && cou.id = 22)"
     "o#(r.id = 17 && sou.id = 18)"
     "b#(name = \"UK4\")"
     "f#(number = nil && b.id = 21)"
     "r#(number = nil && f.id = 22)"
     "aou#(name = \"ПетрГУ\" && cou.id = 23)"
     "aou#(name = \"Public\" && cou.id = 24)"
     "o#(r.id = 19 && sou.id = 20)"
     "b#(name = \"UK5\")"
     "f#(number = nil && b.id = 23)"
     "r#(number = nil && f.id = 24)"
     "aou#(name = \"ПетрГУ\" && cou.id = 25)"
     "aou#(name = \"Public\" && cou.id = 26)"
     "o#(r.id = 21 && sou.id = 22)"
     "b#(name = \"UK6\")"
     "f#(number = nil && b.id = 25)"
     "r#(number = nil && f.id = 26)"
     "aou#(name = \"ПетрГУ\" && cou.id = 27)"
     "aou#(name = \"Public\" && cou.id = 28)"
     "o#(r.id = 23 && sou.id = 24)"
     "b#(name = \"UK8\")"
     "f#(number = nil && b.id = 27)"
     "r#(number = nil && f.id = 28)"
     "aou#(name = \"ПетрГУ\" && cou.id = 29)"
     "aou#(name = \"Public\" && cou.id = 30)"
     "o#(r.id = 25 && sou.id = 26)"
     "b#(name = \"UK8\")"
     "f#(number = nil && b.id = 29)"
     "r#(number = nil && f.id = 30)"
     "aou#(name = \"ПетрГУ\" && cou.id = 31)"
     "aou#(name = \"Public\" && cou.id = 32)"
     "o#(r.id = 27 && sou.id = 28)"
     "b#(name = \"UK9\")"
     "f#(number = nil && b.id = 31)"
     "r#(number = nil && f.id = 32)"
     "aou#(name = \"ПетрГУ\" && cou.id = 33)"
     "aou#(name = \"Public\" && cou.id = 34)"
     "o#(r.id = 29 && sou.id = 30)"
     "b#(name = \"UK9\")"
     "f#(number = nil && b.id = 33)"
     "r#(number = nil && f.id = 34)"
     "aou#(name = \"ПетрГУ\" && cou.id = 35)"
     "aou#(name = \"Гостиница\" && cou.id = 36)"
     "o#(r.id = 31 && sou.id = 32)"
     "b#(name = \"UK9\")"
     "f#(number = 3 && b.id = 41)"
     "r#(number = nil && f.id = 42)"
     "aou#(name = \"ПетрГУ\" && cou.id = 37)"
     "aou#(name = \"Metso\" && cou.id = 38)"
     "o#(r.id = 33 && sou.id = 34)"
     "b#(name = \"MFK\")"
     "f#(number = nil && b.id = 35)"
     "r#(number = nil && f.id = 36)"
     "aou#(name = \"ПетрГУ\" && cou.id = 39)"
     "aou#(name = \"Медицинский факультет\" && cou.id = 40)"
     "o#(r.id = 35 && sou.id = 36)"
     "b#(name = \"TK\")"
     "f#(number = nil && b.id = 37)"
     "r#(number = nil && f.id = 38)"
     "aou#(name = \"ПетрГУ\" && cou.id = 41)"
     "aou#(name = \"Медицинский факультет\" && cou.id = 42)"
     "o#(r.id = 37 && sou.id = 38)"
     "b#(name = \"RB\")"
     "f#(number = nil && b.id = 39)"
     "r#(number = nil && f.id = 40)"
     "aou#(name = \"ПетрГУ\" && cou.id = 42)"
     "aou#(name = \"Медицинский факультет\" && cou.id = 43)"
     "o#(r.id = 39 && sou.id = 40)"
     "li#(description != nil && description != \"\") (n (d))"])


(def tree-queries-jpa
  ^{:doc "JPA queries which are used for building SON tree."}
  ["n#(sou.id = 1)"
   "d#(sou.id = 2 && n.id = 3)"
   "n#(r.id = 4)" 
   "d#(r.id = 5 && n.id = 6)"])
