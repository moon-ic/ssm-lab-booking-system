# 参考文献原文与翻译

> **文献信息：**
> Adekunle A A, Abolore B L, Mutiu G, et al. Design and Implementation of a Web-Based Laboratory Management System for Efficient Resource Tracking[J]. Asian Journal of Electrical Sciences, 2024, 13(2): 19-24.
> DOI: https://doi.org/10.70112/ajes-2024.13.2.4248

---

## 原文（Original Text）

**Design and Implementation of a Web-Based Laboratory Management System for Efficient Resource Tracking**

Abiona Akeem Adekunle\*, Badmus Lukman Abolore, Ganiyu Mutiu and Ajinaja Micheal Olalekan

*Department of Computer Science, Federal Polytechnic Ile-Oluji, Nigeria*

*(Received 8 August 2024; Revised 28 August 2024; Accepted 30 September 2024; Available online 8 October 2024)*

---

**Abstract** — Effective management of laboratory resources and equipment is crucial for academic institutions to support teaching and learning activities. At the Federal Polytechnic, Ile-Oluji, Ondo State, the Computer Science Department faced challenges with manual tracking of ICT gadgets and inefficient laboratory resource management, resulting in errors and delays. This study aims to design and implement a Web-Based Laboratory Management System (WBLMS) to automate the processes of reservation, tracking, and inventory management, thereby enhancing operational efficiency and reducing errors in laboratory resource management. The research employed a system development methodology that included requirement analysis, system design, implementation, and testing. A user-centric approach was adopted to ensure the system meets the specific needs of laboratory staff and students. Key system modules include an administrator login, a dashboard for gadget reservations, transaction and inventory management sections, data visualization features, and a history tracking module. The WBLMS was deployed in the ICT laboratory and underwent initial testing, which demonstrated significant improvements in resource management. The system facilitates real-time monitoring of borrowed equipment, providing details such as model, brand, and quantity. It streamlines operations, reduces manual effort, and minimizes errors, offering an intuitive and automated solution for laboratory management. The implementation of WBLMS enhances the overall efficiency of laboratory operations by providing a user-friendly platform for managing laboratory resources. It ensures accurate record-keeping, reduces human error, and simplifies resource tracking, making it a valuable tool for academic institutions aiming to optimize their laboratory management processes.

**Keywords:** Laboratory Management, Web-Based Laboratory Management System (WBLMS), Inventory Management, Resource Tracking

---

### I. INTRODUCTION

Laboratory management plays a crucial role in the smooth operation of academic institutions, particularly in technical and computer science departments, where the efficient handling of ICT resources is essential for practical learning and research. In many institutions, laboratory management is still handled manually, resulting in inefficiencies such as resource mismanagement, data entry errors, and difficulties in tracking borrowed equipment. This problem is prevalent at the Federal Polytechnic, Ile-Oluji, Ondo State, where the current system relies heavily on manual processes that are time-consuming, prone to errors, and lack real-time monitoring.

This study aims to design and implement a Web-Based Laboratory Management System (WBLMS) that automates the management of laboratory resources in the ICT laboratory of the Computer Science Department. The proposed system provides a more efficient, reliable, and user-friendly platform for laboratory staff and students. The scope of this study includes the development of a web-based application that streamlines laboratory operations by offering functionalities such as real-time tracking of borrowed ICT equipment, automated inventory management, resource reservation, and detailed monitoring of equipment usage.

The WBLMS is designed with multiple modules to facilitate user login, transaction management, borrower and item records, and data visualization through graphical reports. Additional features include room allocation and historical tracking of laboratory activities, ensuring comprehensive resource management. The significance of this study lies in its potential to transform laboratory resource management in academic institutions. By automating inventory and reservation processes, the WBLMS reduces manual effort and minimizes the risk of human error. This leads to increased efficiency, better resource utilization, and improved user satisfaction.

Furthermore, the system provides laboratory administrators with real-time data on equipment status, enabling informed decision-making and effective management of ICT resources. Similar studies have demonstrated that implementing web-based management systems in educational environments enhances operational efficiency and resource accountability [1], [2]. This research addresses the specific challenges faced by the Federal Polytechnic, Ile-Oluji, while contributing to the growing body of knowledge on the use of information technology in educational resource management. By offering a practical solution tailored to the institution's needs, this study illustrates how ICT innovations can improve operational processes in academic settings.

---

### II. REVIEW OF LITERATURE

In the domain of web-based laboratory management systems (LMS), significant progress has been made in automating various aspects of laboratory functions, including resource tracking, equipment monitoring, and user management. Various studies have highlighted the effectiveness of LMS in enhancing the operational efficiency of laboratories, particularly in educational and research institutions.

One study [3] emphasized the role of performance testing in laboratory environments and introduced a model for web-based systems aimed at optimizing software deployment and resource management. This approach, similar to LMS, underscores the need for accurate real-time performance assessment, especially in technical laboratories, such as the proposed ICT laboratory at the Federal Polytechnic, Ile-Oluji. A related study [4] on web-based remote laboratory systems provided insights into how LMS can support remote laboratory work, particularly for embedded systems. This model demonstrated how students and researchers can conduct experiments remotely, a key advantage in resource-limited settings.

Furthermore, research by P. Rajesh et al., [5] discussed the design of distance laboratories for engineering education, focusing on web-based software and hardware integration. This study parallels the Federal Polytechnic project by highlighting the importance of online platforms for managing complex technical resources and offering detailed user interfaces for system administrators. Another relevant study [6] explored how virtual tours and building information models (BIMs) can be integrated into laboratory management systems using IoT technologies. Their system enhanced facility management by enabling real-time monitoring of laboratory assets, which is directly relevant to the proposed system for ICT gadgets and borrower tracking.

Lastly, Hua Jiang et al., [7] proposed a state-driven approach to resource management within distributed web applications, focusing on scalable deployment across laboratory systems. This model could inform the design of the WBLMS for the Computer Science Department's ICT laboratory by offering methods for handling large datasets and managing multiple users simultaneously. Each of these works provides valuable perspectives on the design and implementation of web-based laboratory systems, emphasizing resource management, automation, and user interface design — core components of the proposed WBLMS for the Federal Polytechnic's ICT laboratory.

---

### III. METHODOLOGY

The methodology for the design and implementation of the Web-Based Laboratory Management System (WBLMS) for the Federal Polytechnic, Ile-Oluji, Ondo State, is divided into several key stages to ensure the software meets the institution's specific needs and efficiently manages ICT laboratory resources.

#### A. System Requirements Gathering

1. **Stakeholder Engagement:** This phase involves interacting with key stakeholders, such as ICT lab administrators, lecturers, and students, to identify requirements for managing laboratory resources. The primary data collected include the types of ICT gadgets (e.g., computers, projectors, printers) in the lab, the expected borrowing process, inventory tracking, and user management.

2. **Requirements Documentation:** All functional and non-functional requirements are documented. Functional requirements include user authentication (admin and borrower login), gadget reservation, gadget inventory management, and transaction records. Non-functional requirements include system performance, scalability, and security.

#### B. System Design

1. **Database Design:** The system requires a relational database for storing information about items, transactions, users, rooms, and borrower details. The database schema is structured with key tables such as:
   - a. Users: Stores login credentials, roles (admin, borrower), and personal details.
   - b. Inventory: Keeps details of available ICT gadgets (e.g., model, brand, quantity).
   - c. Transactions: Tracks gadgets borrowed and returned, along with timestamps.
   - d. Borrowers: Stores borrower information, including department, ID, and history.
   - e. Rooms: Stores information about rooms where gadgets are located.

#### C. User Interface Design

The system's user interface is divided into different sections: Login, Dashboard, Transactions, Inventory Management, Borrowers, Rooms, User Management, and History. Each section provides specific functionality for administrators to manage laboratory operations effectively.

1. **Admin Dashboard:** Provides an overview of reservations, gadget availability, and system usage.
2. **Transactions Module:** Tracks the borrowing and returning of gadgets, including due dates.

#### D. System Development

1. **Front-End Development:** The front end is developed using web technologies such as HTML, CSS, and JavaScript for dynamic content and interactivity. The Bootstrap framework is used to ensure responsive design for both mobile and desktop use.

2. **Back-End Development:** The back end is developed using PHP and MySQL for the database. PHP is chosen for server-side scripting to handle user requests, manage sessions, and interact with the database. APIs are developed to facilitate communication between the front end and back end for operations such as fetching inventory data, updating transaction logs, and managing user sessions.

3. **Authentication and Authorization:** JWT (JSON Web Token) or session-based authentication is implemented to manage user login and ensure that only authorized users (e.g., administrators) can perform sensitive operations, such as adding new items to the inventory or viewing transaction histories.

#### E. Testing and Validation

1. **Unit Testing:** Each module is tested individually to ensure functionality, including login, data retrieval, and gadget reservation processes.
2. **Integration Testing:** This phase ensures that all components (database, front end, and back end) work together seamlessly.
3. **User Acceptance Testing (UAT):** The system is deployed in a test environment, where stakeholders, such as lab administrators, validate whether the system meets their requirements.

#### F. Deployment and Maintenance

1. **Deployment:** The system is deployed on a Linux-based server and is accessible via a web browser over the institution's local intranet or the internet, depending on the configuration. Apache is used as the web server, and MySQL as the database.

2. **Maintenance:** Regular maintenance is performed to ensure system performance and security. Updates are rolled out periodically based on user feedback or changing requirements.

#### G. Architecture of the System

1. **User Interface:** The front-end interface allows administrators to log in and manage the laboratory's inventory, borrowers, and transactions. The design is user-friendly, providing quick access to key system features such as reservations, transaction logs, and inventory status.

2. **Application Layer:** This layer contains the core business logic, processing user requests such as fetching inventory details or recording new transactions. It communicates with the database and handles authentication, ensuring that only authorized users can access or manipulate data.

3. **Database Layer:** This layer stores all information related to ICT gadgets, users, and transactions in the MySQL database. It is essential for persistent data storage and retrieval during system operations.

---

### IV. DISCUSSION OF THE STUDY

The diagram illustrates the login interface of the Laboratory Management System, designed with simplicity and usability as primary considerations. Users are prompted to enter their username and password to ensure secure access. The layout features a clean design, with a prominent title at the top, followed by input fields and a "Log In" button. The inclusion of the institution's logo personalizes the system and reinforces its institutional identity. Additionally, a link at the bottom of the interface provides access to a member's page, catering to potential non-admin users.

#### A. Dashboard

The dashboard is a key component of the Laboratory Management System, providing administrators with a comprehensive overview to manage ICT laboratory resources efficiently. A sidebar on the left offers various navigation options, including Dashboard, Transaction, Item, Borrower, Room, Inventory, Graph, User, and History, allowing users to switch quickly between different system functionalities.

The main panel displays an inventory table listing ICT gadgets in the lab. Each entry includes the Model, Category (e.g., AVR, TV, Projector), Brand, Quantity, and Status of the gadgets. Administrators can export the list in formats such as CSV, Excel, PDF, or print it directly. A search bar in the top-right corner enables quick filtering of items within the inventory. This streamlined layout allows lab administrators to easily track the availability and status of all ICT equipment.

#### B. Navigation Panel

The left sidebar of the Laboratory Management System (LMS) dashboard provides navigation options for accessing various features and functionalities:

1. **Dashboard:** The central hub that provides an overview of key metrics and information, such as recent transactions, inventory status, and system notifications.
2. **Transaction:** Tracks all lending and borrowing activities within the laboratory. Each transaction logs details such as the borrower, the item borrowed, the quantity, and the borrowing/return dates.
3. **Item:** Manages all items or gadgets available in the ICT laboratory. Administrators can add, modify, or remove items. Each item entry records information such as model, category, brand, and quantity.
4. **Borrower:** Manages details about individuals authorized to borrow equipment from the lab. It stores information such as the borrower's name, department, and borrowing history.
5. **Room:** Tracks and manages different rooms or locations within the lab or institution. This module allows administrators to allocate and assign equipment to specific rooms, ensuring proper resource distribution and tracking.
6. **Inventory:** Provides a detailed overview of all lab equipment, including current quantities and statuses (e.g., new, in-use, or damaged). Administrators can use this tab to manage stock levels in real time.
7. **Graph:** Displays visual reports and analytics on lab usage, such as frequently borrowed items, inventory trends, and other key metrics. Graphical representations enable quick interpretation of data.
8. **User:** Manages system users and their roles (e.g., administrators, staff). Administrators can add or remove users and assign privileges, controlling access to different sections of the LMS.
9. **History:** Maintains a log of all past actions within the system, including previous transactions, item modifications, and changes to borrower information. This feature is essential for auditing and tracking the historical use of lab resources.

#### C. Relational Database Design

The system uses a relational database with several interconnected tables:

1. **Borrow** (lms19.borrow): Tracks borrowing transactions — date_borrow, borrowed item identifier, member_id, stock_id, quantity, status, and date_return.
2. **Room Equipment** (lms19.room_equipment): Represents equipment assigned to rooms — equipment_id, room_id, quantity.
3. **History Logs** (lms19.history_logs): Logs actions for auditing — description, table_name, status_name, user_type, user_id, date_created.
4. **Equipment Inventory** (lms19.equipment_inventory): Holds equipment information — equipment_id, remarks, status.
5. **Member** (lms19.member): Represents system users — id, name, school, contact, gender, department, password, status.
6. **Item Inventory** (lms19.item_inventory): Manages stock-level information — inventory_itemstock, item_remarks, date_change.
7. **Room** (lms19.room): Stores room information — id, room_name, status, date_added.
8. **Item Transfer** (lms19.item_transfer): Tracks movement of items — item_id, roomID, quantity, date_transfer, person_in_charge.
9. **Item** (lms19.item): Represents individual equipment — deviceID, category, brand, description, type, status.
10. **User** (lms19.user): Tracks system users (admin) — username, password, status.
11. **Reservation** (lms19.reservation): Manages reservations — reservation_code, member_id, stock_id, room_id, assign, status, remarks.
12. **Reservation Status** (lms19.reservation_status): Tracks reservation status — reservation_code, remark.
13. **Relationships:** The system relies heavily on foreign keys to link data across tables. For instance, Borrow links to Member (via member_id) and Item (via stock_id); Room Equipment links Room and Equipment; Reservation is tied to Members, Items, and Rooms.

Overall, this Entity-Relationship Diagram (ERD) is designed to manage an equipment borrowing and reservation system with detailed tracking of inventory, transfers, and user interactions.

---

### V. CONCLUSION

In conclusion, the Web-Based Laboratory Management System (WBLMS) designed for the ICT Laboratory at the Federal Polytechnic, Ile-Oluji, offers a streamlined and efficient solution for managing laboratory resources. By integrating essential features such as inventory management, transaction tracking, borrower records, and real-time status updates, the system significantly reduces the administrative workload while enhancing the accuracy of laboratory operations. The user-friendly interface ensures that administrators can easily monitor equipment usage, track borrowings, and maintain updated records, thereby improving overall accountability and resource management.

Moreover, the system's ability to generate detailed reports and visual representations of data facilitates better decision-making, enabling timely interventions when resources are running low or require maintenance. By deploying the WBLMS, the ICT Laboratory can optimize the availability and management of its equipment, fostering a more organized and efficient environment for both staff and students. The case study of the Federal Polytechnic, Ile-Oluji, demonstrates the system's practicality and scalability, making it adaptable to other institutions with similar needs. Future enhancements could involve integrating more advanced features, such as predictive maintenance alerts or incorporating artificial intelligence to further streamline operations and enhance the user experience. Overall, the WBLMS is a valuable tool for ensuring effective management of laboratory resources in academic environments.

---

### REFERENCES

[1] G. Ru and L. X. Long, "Development and application of multimedia network resources platform," 2012 2nd International Conference on Consumer Electronics, Communications and Networks (CECNet), Yichang, China, 2012, pp. 272-275.

[2] C. P. Weinthal, M. M. Larrondo-Petrie, and L. F. Zapata-Rivera, "Academic integrity assurance methods and tools for laboratory settings," 2019 IEEE Frontiers in Education Conference (FIE), Covington, KY, USA, 2019, pp. 1-6.

[3] S. Ravichandran and M. Umamaheswari, "Innovative method of software testing environment in cloud computing technology," Asian Journal of Computer Science and Technology, vol. 3, no. 2, pp. 34-39, Oct. 2014.

[4] O. D. Adekola, "An online road transport booking system," Asian Journal of Computer Science and Technology, vol. 10, no. 2, pp. 1-5, Jul. 2021.

[5] P. Rajesh, A. Valarmathi, K. Sathishkumar, and S. Yoga Vignesh, "Secured e-counselling framework using composite web services," Asian Journal of Computer Science and Technology, vol. 1, no. 1, pp. 118-120, May 2012.

[6] V. Bhagyasree, K. Rohitha, K. Kusuma, and S. Kokila, "Big data integration with IoT to achieve the challenges," Asian Journal of Computer Science and Technology, vol. 8, no. S3, pp. 45-49, Apr. 2019.

[7] H. Jiang and X. Zhao, "Study on the lab teaching system construction of information management and information system profession," 2010 International Conference on E-Health Networking Digital Ecosystems and Technologies (EDT), Shenzhen, 2010, pp. 364-366.

---

## 翻译（中文翻译）

**基于 Web 的实验室管理系统设计与实现——高效资源跟踪**

Abiona Akeem Adekunle\*、Badmus Lukman Abolore、Ganiyu Mutiu、Ajinaja Micheal Olalekan

*尼日利亚伊莱奥卢吉联邦理工学院计算机科学系*

---

**摘要** — 对学术机构而言，有效管理实验室资源和设备是支撑教学活动的重要基础。奥南多州伊莱奥卢吉联邦理工学院计算机科学系在 ICT 设备手工管理方面面临诸多挑战，实验室资源管理效率低下，导致差错频发、操作延误。本研究旨在设计并实现一套基于 Web 的实验室管理系统（WBLMS），对预约、跟踪与库存管理流程进行自动化处理，从而提升运营效率、减少实验室资源管理中的错误。研究采用包含需求分析、系统设计、开发实现与测试在内的系统开发方法论，并以用户为中心，确保系统满足实验室工作人员和学生的实际需求。系统核心模块包括管理员登录、设备预约仪表盘、交易与库存管理板块、数据可视化功能及历史记录追踪模块。WBLMS 部署于 ICT 实验室并完成初步测试，结果表明资源管理效率得到显著提升。系统可对借出设备进行实时监控，提供型号、品牌和数量等详细信息，有效简化操作流程、减少人工操作、降低出错率，为实验室管理提供了直观、自动化的解决方案。WBLMS 的实施通过提供友好的资源管理平台，全面提升了实验室的运营效率，确保记录准确、降低人为差错、简化资源跟踪，是学术机构优化实验室管理流程的有力工具。

**关键词：** 实验室管理；基于 Web 的实验室管理系统（WBLMS）；库存管理；资源跟踪

---

### 一、引言

实验室管理对学术机构的正常运转至关重要，尤其是在技术类和计算机科学系，ICT 资源的高效管理是实践教学与科研的基础保障。目前，许多机构的实验室管理仍依赖人工方式，存在资源管理混乱、数据录入出错、借出设备难以追踪等问题。这一问题在伊莱奥卢吉联邦理工学院尤为突出，现行系统高度依赖人工流程，不仅耗时耗力，还容易出错，且缺乏实时监控能力。

本研究旨在设计并实现一套 Web 实验室管理系统（WBLMS），对计算机科学系 ICT 实验室的资源管理实现自动化。该系统为实验室工作人员和学生提供更高效、可靠且友好的操作平台。研究范围包括开发一套 Web 应用，通过提供 ICT 借出设备实时跟踪、自动化库存管理、资源预约、设备使用情况详细监控等功能，全面优化实验室日常运营。

WBLMS 包含多个模块，支持用户登录、事务管理、借用人及物品记录，并通过图形化报表实现数据可视化。此外还提供房间分配和实验室活动历史追踪功能，确保资源管理全面覆盖。本研究的重要意义在于其将实验室资源管理模式转型的潜力——通过自动化库存与预约流程，WBLMS 减少了人工操作，降低了人为差错的风险，从而提升效率、优化资源利用、改善用户体验。系统还能为实验室管理员提供设备状态的实时数据，支持科学决策和 ICT 资源的有效管理。

---

### 二、文献综述

在基于 Web 的实验室管理系统（LMS）领域，实验室功能自动化已取得显著进展，涵盖资源跟踪、设备监控和用户管理等多个方面。多项研究表明，LMS 能够有效提升各类教育和科研机构实验室的运营效率。

文献 [3] 着重分析了实验室环境中性能测试的作用，提出了一种旨在优化软件部署和资源管理的 Web 系统模型，强调了实时性能评估在技术实验室中的重要性。文献 [4] 对基于 Web 的远程实验室系统进行了研究，揭示了 LMS 如何支持远程实验工作，在资源受限环境中具有明显优势。P. Rajesh 等人 [5] 探讨了工程教育远程实验室的设计，聚焦于 Web 软硬件集成，强调了在线平台管理复杂技术资源的重要性。文献 [6] 则研究了将虚拟漫游与建筑信息模型（BIM）结合物联网技术集成到实验室管理系统中的方法，通过资产实时监控提升了设施管理水平。Hua Jiang 等人 [7] 提出了分布式 Web 应用中资源管理的状态驱动方法，重点关注实验室系统的可扩展部署，为处理大数据集和多用户并发提供了参考思路。上述研究均从资源管理、自动化和用户界面设计等核心维度，为 Web 实验室管理系统的设计与实现提供了宝贵借鉴。

---

### 三、研究方法

WBLMS 的设计与实现方法论分为若干关键阶段，以确保软件满足机构的具体需求，并高效管理 ICT 实验室资源。

#### A. 系统需求收集

1. **利益相关方访谈**：通过与 ICT 实验室管理员、讲师和学生等关键利益相关方交流，收集实验室资源管理需求，包括实验室中 ICT 设备类型（如计算机、投影仪、打印机）、借用流程预期、库存跟踪及用户管理需求。

2. **需求文档化**：对所有功能性和非功能性需求进行文档记录。功能性需求包括用户身份验证（管理员和借用人登录）、设备预约、库存管理和交易记录；非功能性需求涉及系统性能、可扩展性和安全性。

#### B. 系统设计

1. **数据库设计**：系统采用关系型数据库存储物品、交易、用户、房间和借用人信息。核心数据表包括：
   - **用户表**：存储登录凭证、角色（管理员、借用人）和个人信息。
   - **库存表**：记录可用 ICT 设备详情（型号、品牌、数量）。
   - **交易表**：跟踪设备借出和归还记录及时间戳。
   - **借用人表**：存储借用人信息（所属院系、ID 和历史记录）。
   - **房间表**：存储设备所在房间信息。

#### C. 用户界面设计

系统界面分为登录、仪表盘、交易、库存管理、借用人、房间、用户管理和历史记录等模块，各模块为管理员提供相应的实验室管理功能。

#### D. 系统开发

1. **前端开发**：前端采用 HTML、CSS 和 JavaScript 实现动态内容和交互功能，Bootstrap 框架确保响应式设计，兼容移动端和桌面端。

2. **后端开发**：后端使用 PHP 和 MySQL 数据库。PHP 负责服务器端脚本处理、会话管理和数据库交互。开发 API 以实现前后端通信，支持库存数据查询、交易日志更新和用户会话管理等操作。

3. **认证与授权**：采用 JWT（JSON Web Token）或基于会话的身份验证机制管理用户登录，确保只有授权用户（如管理员）才能执行敏感操作（如新增库存或查看交易历史）。

#### E. 测试与验证

1. **单元测试**：对各模块（包括登录、数据查询和设备预约流程）进行独立功能测试。
2. **集成测试**：验证数据库、前端和后端等所有组件能够无缝协同工作。
3. **用户验收测试（UAT）**：在测试环境中部署系统，由实验室管理员等利益相关方验证系统是否满足需求。

#### F. 部署与维护

1. **部署**：系统部署于 Linux 服务器，可通过 Web 浏览器经机构内部局域网或互联网访问，采用 Apache 作为 Web 服务器，MySQL 作为数据库。
2. **维护**：定期执行系统维护以确保性能和安全，根据用户反馈或需求变化定期发布更新。

#### G. 系统架构

1. **用户界面层**：前端界面供管理员登录后管理实验室库存、借用人和交易记录，设计友好，可快速访问预约、交易日志和库存状态等核心功能。
2. **应用层**：包含核心业务逻辑，处理用户请求（如获取库存详情或记录新交易），与数据库通信并负责身份验证，确保只有授权用户才能访问或操作数据。
3. **数据库层**：通过 MySQL 数据库持久化存储 ICT 设备、用户和交易相关的所有信息，是系统运行中数据存取的核心基础。

---

### 四、系统功能讨论

系统登录界面以简洁易用为设计原则，用户需输入用户名和密码以保障安全访问，界面布局清晰，顶部为系统标题，其下为输入框和登录按钮，底部链接提供普通用户入口。

#### A. 仪表盘

仪表盘是系统的核心界面，为管理员提供全面的 ICT 实验室资源管理概览。左侧导航栏包含仪表盘、交易、物品、借用人、房间、库存、图表、用户和历史记录等选项，便于用户在各功能模块间快速切换。主面板以表格形式展示实验室 ICT 设备清单，每条记录包含型号、类别（如 AVR、电视、投影仪）、品牌、数量和状态，支持导出 CSV、Excel、PDF 格式或直接打印，并提供搜索栏以快速筛选设备。

#### B. 导航面板各模块说明

1. **仪表盘**：核心概览页面，展示近期交易、库存状态和系统通知，快速呈现实验室运营全貌。
2. **交易**：跟踪实验室内所有借还活动，记录借用人、借用物品、数量及借还日期，确保 ICT 设备使用记录准确。
3. **物品**：管理 ICT 实验室所有设备，管理员可添加、修改或删除物品，每条记录包含型号、类别、品牌和数量。
4. **借用人**：管理有权借用设备的用户信息，包括姓名、所属院系和借用历史。
5. **房间**：跟踪管理实验室内的不同房间或区域，支持设备按房间分配，确保资源合理分布和追踪。
6. **库存**：提供所有实验室设备的详细概览，包括当前数量和状态（如全新、在用或损坏），支持实时库存管理。
7. **图表**：以可视化方式展示实验室使用报告和分析数据，如高频借用设备、库存趋势等，便于快速解读数据。
8. **用户**：管理系统用户及其角色（如管理员、工作人员），可添加或删除用户、分配权限。
9. **历史记录**：保存系统内所有历史操作日志，包括历史交易、物品变更和借用人信息修改，为审计和资源使用追踪提供依据。

#### C. 关系型数据库设计

系统采用多张相互关联的数据表：借用表（跟踪借还交易）、房间设备表（管理房间设备分配）、历史日志表（记录审计日志）、设备库存表、用户表、物品库存表、房间表、物品转移表、物品表、系统用户表、预约表和预约状态表。各表通过外键紧密关联，共同构成一套完整的设备借还与预约管理体系，实现库存、转移和用户交互的全面追踪。

---

### 五、结论

综上所述，为伊莱奥卢吉联邦理工学院 ICT 实验室设计的 WBLMS，为实验室资源管理提供了一套流程清晰、运营高效的解决方案。通过整合库存管理、交易跟踪、借用人记录和实时状态更新等核心功能，系统在显著减轻行政工作负担的同时提升了实验室操作的准确性。友好的用户界面使管理员能够便捷地监控设备使用情况、跟踪借还记录并维护最新台账，从而全面提升问责能力和资源管理水平。

此外，系统强大的报表生成和数据可视化功能有助于管理者做出更科学的决策，在资源紧缺或需要维护时及时介入处置。WBLMS 的部署使 ICT 实验室能够优化设备可用性和管理水平，为师生创造更有序、高效的实验环境。以伊莱奥卢吉联邦理工学院为代表的实践案例验证了系统的实用性和可扩展性，表明该方案可推广至有类似需求的其他机构。未来可进一步集成预测性维护预警或引入人工智能等高级功能，持续优化操作流程、提升用户体验。总体而言，WBLMS 是学术机构实现实验室资源有效管理的有力工具。

---

## 来源

- [Design and Implementation of a Web-Based Laboratory Management System for Efficient Resource Tracking — Asian Journal of Electrical Sciences](https://ajesjournal.org/index.php/ajes/article/view/4248)
