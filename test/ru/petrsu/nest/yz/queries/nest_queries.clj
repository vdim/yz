
;;
;; Copyright 2011 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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
  
(defmacro is-list?
  [queries]
  (concat
    `(do)
    (for [query (eval queries)]
      `(deftest ~(gensym)
                (is (tc/rows-query ~query))))))


(is-list? address-info-queries)
(is-list? enlivener-queries)
