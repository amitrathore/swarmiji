#!/usr/bin/env ruby

require "rubygems"
require "activerecord"

def usage
  puts "Swarmiji DB creator."
  puts "Usage: schema.rb db-host[localhost|remote.runa.com|etc] db-environment [development|test|production|staging|etc.]"
end

puts "ARGV.length: #{ARGV.length} ARGV: #{ARGV.inspect}"

if ARGV.length != 2
  usage
  exit -1
end

ActiveRecord::Base.establish_connection(
  :adapter => "mysql", 
  :host => ARGV[0],
  :database => "swarmiji_#{ARGV[1]}",
  :username => "cinch", 
  :password => "secret"
)

ActiveRecord::Base.connection.instance_eval do
  create_table "control_messages", :force => true do |t|
    t.string "sevak_server_pid", "message_type", "sevak_name", "sevak_args", "sevak_time", "messaging_time", "return_q_name"
    t.timestamps
  end
end

puts "Created swarmiji_#{ARGV[1]} schema on #{ARGV[0]}."
