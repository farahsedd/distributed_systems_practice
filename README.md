
### Overview
This project implements a distributed sales management system where multiple branch offices (BO1, BO2) handle sales data independently and use RabbitMQ to ensure all data is consistently updated in the head office (HO) central database. The system is designed to avoid conflicts and ensure high availability, allowing for seamless updates across multiple databases.

### Architecture
![diagram](https://drive.google.com/uc?id=1HeakSMKALYBgVtVyD-DUTdlXZ24bqLBp)
- Branch Office 1 (BO1): Stores local sales data in its own database and sends sales updates to RabbitMQ.
- Branch Office 2 (BO2): Similar to BO1, it stores local sales data in its own database and sends updates to RabbitMQ.
- Head Office (HO): Receives sales data updates from RabbitMQ and appends them to the central database, ensuring consistency across the organization.
- RabbitMQ Broker: Acts as the message broker, ensuring reliable and conflict-free transmission of sales data from BO1 and BO2 to the HO.
- Central Database (HO): Stores and manages all sales transactions centrally, maintaining consistency across the organization.

### Key Benefits

**Data Consistency:** Ensures all sales data from multiple branches is correctly and consistently reflected in the central database.

**Conflict-Free Updates:** RabbitMQ helps in managing conflicts, ensuring that sales data from different branches doesn't overwrite each other.

**High Availability:** System remains operational even if one of the branch services is down, as RabbitMQ ensures messages are processed when services are back online.

**Scalability:** New branch offices can be added easily by integrating their services with RabbitMQ and the central database.
