<?php
// Helper file to make MySQLi work with PDO-style code
require_once 'dbConfig.php';

function getDB() {
    global $conn;
    
    // Check if connection is valid
    if ($conn->connect_error) {
        return null;
    }
    
    // Return a wrapper object that mimics PDO
    return new MySQLiWrapper($conn);
}

class MySQLiWrapper {
    private $conn;
    
    public function __construct($connection) {
        $this->conn = $connection;
    }
    
    public function prepare($query) {
        return new MySQLiStmtWrapper($this->conn, $query);
    }
    
    public function lastInsertId() {
        return $this->conn->insert_id;
    }
    
    public function query($sql) {
        return $this->conn->query($sql);
    }
    
    public function exec($sql) {
        return $this->conn->query($sql);
    }
}

class MySQLiStmtWrapper {
    private $conn;
    private $query;
    private $params = [];
    public $result; // Declare the property to avoid deprecated warning
    
    public function __construct($connection, $query) {
        $this->conn = $connection;
        $this->query = $query;
    }
    
    public function execute($params = []) {
        $this->params = $params;
        
        // Replace ? placeholders with escaped values
        $escaped_query = $this->query;
        foreach ($params as $param) {
            $escaped_param = $this->conn->real_escape_string($param);
            $escaped_query = preg_replace('/\?/', "'$escaped_param'", $escaped_query, 1);
        }
        
        $this->result = $this->conn->query($escaped_query);
        return $this->result !== false;
    }
    
    public function fetchAll() {
        if (!$this->result) {
            return [];
        }
        
        $rows = [];
        while ($row = $this->result->fetch_assoc()) {
            $rows[] = $row;
        }
        return $rows;
    }
    
    public function fetch() {
        if (!$this->result) {
            return false;
        }
        return $this->result->fetch_assoc();
    }
    
    public function rowCount() {
        return $this->conn->affected_rows;
    }
}
?>


















